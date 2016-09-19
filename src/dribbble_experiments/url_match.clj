(ns dribbble-experiments.url_match
  (:gen-class)
  (:require [clojure.string :as st]
            [clojure.data.json :as json]))

;;
;; Matcher should recognize and destruct URL by:
;; host: domain
;; path: parts, splitted with "/"
;; queryparam: name/value pairs of query
;; (see examples below)
;; 
;; Each string that is started from "?" is a "bind"
;; (recognize matcher) should return nil or seq of binds
;;

; ======================================================================================================================
; === TASK EXAMPLES ====================================================================================================

; (def twitter (Pattern. "host(twitter.com); path(?user/status/?id);"))
; (recognize twitter "http://twitter.com/bradfitz/status/562360748727611392")
; ;; => [[:id 562360748727611392] [:user "bradfitz"]]

; (def dribbble (Pattern. "host(dribbble.com); path(shots/?id); queryparam(offset=?offset);")
; (recognize dribbble "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1")
; ;; => [[:id "1905065-Travel-Icons-pack"] [:offset "1"]]
; (recognize dribbble "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1")
; ;; => nil ;; host mismatch
; (recognize dribbble "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users")
; ;; => nil ;; offset queryparam missing

; (def dribbble2 (Pattern. "host(dribbble.com); path(shots/?id); queryparam(offset=?offset); queryparam(list=?type);")

; ======================================================================================================================
; === IMPLEMENTATION ===================================================================================================

; ==============================================================================
; === GENERIC ==================================================================

; (defn uri-allowed-chars []
;   "\\w\\-\\~\\%\\.")

(defonce uri-allowed-chars "\\w\\-\\~\\%\\.")

; :type
; from
; "list=?type"
;
; :user
; from
; "?user"
(defn param-to-keyword [pattern]
  (keyword (re-find (re-matcher (re-pattern (str "(?<=\\?)[" uri-allowed-chars "]+(?=$)")) pattern))))

; "https\\:\\/\\/dribbble\\.com\\/shots\\/1905065\\-Travel\\-Icons\\-pack\\?list\\=users"
; from
; "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users"
;
; "my\\-domain\\.com\\.eu"
; from
; "my-domain.com.eu"
(defn prepare-string-to-re-pattern [original-string]
  (st/replace original-string #"\W" #(str "\\" %1)))

; ==============================================================================
; === HOST =====================================================================

; "dribbble.com"
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn host-name [pattern]
  (re-find (re-matcher (re-pattern (str "(?<=host\\()[" uri-allowed-chars "]+(?=\\))")) pattern)))

; #"^(http|https)://dribbble\.com($|\/$|\/.+|\#$|\#.+)"
; from
; "dribbble.com"
(defn host-pattern [host-name]
  (re-pattern (str "^(http|https)://" (prepare-string-to-re-pattern host-name) "($|\\/$|\\/.+|\\#$|\\#.+)")))

; #"^(http|https)://dribbble\.com($|\/$|\/.+|\#$|\#.+)"
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn host-info [pattern]
  (let [inner-host-name (host-name pattern)]
    (if (some? inner-host-name) (host-pattern inner-host-name))))

; true
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);")
; "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"
;
; false
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);")
; "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"
(defn check-host-by-info [pattern uri]
  (some? (re-matches pattern uri)))

; ==============================================================================
; === PATH =====================================================================

; "?user/status/?id"
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn path [pattern]
  (re-find (re-matcher (re-pattern (str "(?<=path\\()[" uri-allowed-chars "\\?\\/]+(?=\\))")) pattern)))

; ["?user" "status" "?id"]
; from
; "?user/status/?id"
(defn path-to-pieces [path-pattern]
  (st/split path-pattern #"/"))

; "status"
; from
; "status"
;
; nil
; from
; "?status"
(defn path-piece-name [path-piece]
  (re-matches (re-pattern (str "[" uri-allowed-chars "]+")) path-piece))

; [[0 :user nil] [1 nil "status"] [2 :id nil]]
; from
; "?user/status/?id"
(defn path-param-info [path-pattern]
  (let [path-pieces (path-to-pieces path-pattern)]
    (vec (map-indexed (fn [num item] [num (param-to-keyword item) (path-piece-name item)]) path-pieces))))

; #"(?<=^(?:http|https)://dribbble\.com\/)[\w\-\~\%\.\/]+(?=\?)"
; from
; "dribbble.com"
(defn path-pattern [host-name]
  (re-pattern (str "(?<=^(?:http|https)://" (prepare-string-to-re-pattern host-name) "\\/)[" uri-allowed-chars "\\/]+(?=\\?)")))

; {:path-pattern #"(?<=^(?:http|https)://dribbble\.com\/)[\w\-\~\%\.\/]+(?=\?)", :path-pieces [[0 :user nil] [1 nil "status"] [2 :id nil]]}
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn path-info [pattern]
  (let [inner-path (path pattern)
        inner-host-name (host-name pattern)]
    (if (some? inner-path) {
      :path-pattern (path-pattern inner-host-name)
      :path-pieces (path-param-info inner-path)})))

; (defn check-passive-path-piece [passive-path-piece-template name-to-check])

; "shots/1905065-Travel-Icons-pack"
; from
; "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"
; #"(?<=^(?:http|https)://dribbble\.com\/)[\w\-\~\%\.\/]+(?=\?)"
(defn get-path-with-info [uri pattern]
  (re-find (re-matcher pattern uri)))

; [[:user "some-username"] [:id "1905065-Travel-Icons-pack"]]
; from
; "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1"
; {:path-pattern #"(?<=^(?:http|https)://dribbble\.com\/)[\w\-\~\%\.\/]+(?=\?)", :path-pieces [[0 :user nil] [1 nil "status"] [2 :id nil]]}
(defn generate-path-answer [uri inner-path-info]
  (let [can-be? (some? inner-path-info)
        inner-relative-part (if can-be? (get-path-with-info uri (:path-pattern inner-path-info)))
        inner-path-pieces (if (some? inner-relative-part) (path-to-pieces inner-relative-part))
        inner-pieces-info (if can-be? (:path-pieces inner-path-info))
        inner-is-enough-pieces? (if can-be? (>= (count inner-path-pieces) (count inner-pieces-info)) false)
        inner-is-good-to-go? (and can-be? inner-is-enough-pieces?)]
    (if inner-is-good-to-go?
      (remove nil? (vec (map #((fn [path-pieces piece-info] 
          (if (some? (get piece-info 1)) 
            [
              (get piece-info 1) 
              (get path-pieces (get piece-info 0))
            ]
            (if (not (= (get piece-info 2) (get path-pieces (get piece-info 0))))
              [nil nil])
          )
        ) inner-path-pieces %) inner-pieces-info)))
      )))

; ==============================================================================
; === QUERYPARAMS ==============================================================

; ["offset=?offset" "list=?type"]
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn queryparams [pattern]
  (vec (re-seq (re-pattern (str "(?<=queryparam\\()[" uri-allowed-chars "\\?\\=\\/]+(?=\\))")) pattern)))

; "list"
; from
; "list=?type"
(defn query-param-name [pattern]
  (re-find (re-matcher (re-pattern (str "(?<=^)[" uri-allowed-chars "]+(?=\\=\\?)")) pattern)))

; #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&\#])"
; from
; "list"
(defn query-param-pattern [q-param-name]
  (re-pattern (str "(?<=[\\?\\&]" q-param-name "\\=)[\\w\\-\\%]+(?=[$\\&\\#])")))

; [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&\#])"]
; from
; "list=?type"
(defn query-param-info [pattern]
  [(param-to-keyword pattern) (query-param-pattern (query-param-name pattern))])

; [[:offset #"(?<=[\?\&]offset\=)[\w\-\%]+(?=[$\&\#])"] [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&\#])"]]
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn query-info [pattern]
  (let [inner-queryparams (queryparams pattern)]
    (if (some? inner-queryparams) (vec (map query-param-info inner-queryparams)))))

; "users"
; from
; "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"
; #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&\#])"
(defn get-query-param-with-info [uri pattern]
  (re-find (re-matcher pattern uri)))

; [[:list "users"] [:offset "1"]]
; from
; "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1#some-my/fragment"
; [[:offset #"(?<=[\?\&]offset\=)[\w\-\%]+(?=[$\&\#])"] [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&\#])"]]
(defn generate-query-answer [uri inner-query-info]
  (if (some? inner-query-info)
    (vec (map #((fn [uri param-info] [(get param-info 0) (get-query-param-with-info uri (get param-info 1))]) uri %) inner-query-info))))

; ==============================================================================
; === FRAGMENT ==============================================================

; "?paragraph"
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"
(defn fragment [pattern]
  (re-find (re-matcher (re-pattern (str "(?<=fragment\\()[" uri-allowed-chars "\\?]+(?=\\))")) pattern)))

; #"(?<=\#).+(?=$)"
(defn fragment-pattern []
  #"(?<=\#).+(?=$)")

; [:paragraph #"(?<=\#).+(?=$)"]
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"
(defn fragment-info [pattern]
  (let [inner-fragment (fragment pattern)]
    (if (some? inner-fragment) [(param-to-keyword inner-fragment) (fragment-pattern)])))

; "some-my/fragment"
; from
; "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1#some-my/fragment"
; #"(?<=\#).+(?=$)"
(defn get-fragment-with-info [uri pattern]
  (re-find (re-matcher pattern uri)))

; [[:paragraph "some-my/fragment"]]
; from
; "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1#some-my/fragment"
; [:paragraph #"(?<=\#).+(?=$)"]
(defn generate-fragment-answer [uri inner-fragment-info]
  (if (some? inner-fragment-info)
    [[(get inner-fragment-info 0) (get-fragment-with-info uri (get inner-fragment-info 1))]]))

; ==============================================================================
; === API ======================================================================

; {:host #"^(http|https)://dribbble\.com($|\/$|\/.+|\#$|\#.+)", :path {:path-pattern #"(?<=^(?:http|https)://dribbble\.com\/)[\w\-\~\%\.\/]+(?=\?)", :path-pieces [[0 :user nil] [1 nil "status"] [2 :id nil]]}, :query [[:offset #"(?<=[\?\&]offset\=)[\w\-\%]+(?=[$\&\#])"] [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&\#])"]], :fragment [:paragraph #"(?<=\#).+(?=$)"]}
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"
(defn pattern-info [pattern]
  { :host (host-info pattern)
    :path (path-info pattern)
    :query (query-info pattern)
    :fragment (fragment-info pattern)})

; [[:user "some-username"] [:id "1905065-Travel-Icons-pack"] [:offset "1"] [:type "users"] [:paragraph "paragraph=3"]]
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)")
; "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3"
;
; nil
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)")
; "https://twitter.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3"
;
; [[:user "some-username"] [nil nil] [:id "weight"] [:offset "1"] [:type nil] [:paragraph nil]]
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)")
; "https://dribbble.com/some-username/height/weight/1905065-Travel-Icons-pack?listing=users&offset=1&page=34"
(defn recognize-detailed [pattern uri]
  (let [inner-host-info (:host pattern)
        is-host-ok? (if (some? inner-host-info)
                        (check-host-by-info inner-host-info uri)
                        true)
        inner-path-info (:path pattern)
        inner-query-info (:query pattern)
        inner-fragment-info (:fragment pattern)]
    (if is-host-ok?
      (vec (concat
        (generate-path-answer uri inner-path-info)
        (generate-query-answer uri inner-query-info)
        (generate-fragment-answer uri inner-fragment-info))))))

; [[:user "some-username"] [:id "1905065-Travel-Icons-pack"] [:offset "1"] [:type "users"] [:paragraph "paragraph=3"]]
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)")
; "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3"
;
; nil
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)")
; "https://twitter.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3"
;
; nil
; from
; (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)")
; "https://dribbble.com/some-username/height/weight/1905065-Travel-Icons-pack?listing=users&offset=1&page=34"
(defn recognize [pattern uri]
  (let [detailed-result (recognize-detailed pattern uri)]
    (if (some? detailed-result)
        (if (not (contains? (set (flatten detailed-result)) nil))
            detailed-result))))
