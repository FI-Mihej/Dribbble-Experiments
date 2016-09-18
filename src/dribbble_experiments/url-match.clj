(ns dribbble-experiments.url-match
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

; :type
; from
; "list=?type"
;
; :user
; from
; "?user"
(defn param-to-keyword [pattern]
  (keyword (re-find (re-matcher #"(?<=\?)[\w\-]+(?=$)" pattern))))

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
  (re-find (re-matcher #"(?<=host\()[\w\.\-]+(?=\))" pattern)))

; #"(?<=^(http|https)://)dribbble\.com[$|\/.+|]"
; from
; "dribbble.com"
(defn host-pattern [host-name]
  (re-pattern (str "^(http|https)://" (prepare-string-to-re-pattern host-name) "($|\\/$|\\/.+)")))

; #"^(http|https)://dribbble\.com($|\/$|\/.+)"
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn host-info [pattern]
  (host-pattern (host-name pattern)))

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
  (re-find (re-matcher #"(?<=path\()[\w\.\-\?\/]+(?=\))" pattern)))

; ["?user" "status" "?id"]
; from
; "?user/status/?id"
(defn path-to-pieces [path-pattern]
  (st/split path-pattern #"/"))

; [[0 :user] [1 nil] [2 :id]]
; from
; "?user/status/?id"
(defn path-param-info [path-pattern]
  (let [path-pieces (path-to-pieces path-pattern)]
    (vec (map-indexed (fn [num item] [num (param-to-keyword item)]) path-pieces))))

; [[0 :user] [1 nil] [2 :id]]
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn path-info [pattern]
  (path-param-info (path pattern)))

; ==============================================================================
; === QUERYPARAMS ==============================================================

; ["offset=?offset" "list=?type"]
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn queryparams [pattern]
  (vec (re-seq #"(?<=queryparam\()[\w\.\-\?\=]+(?=\))" pattern)))

; "list"
; from
; "list=?type"
(defn query-param-name [pattern]
  (re-find (re-matcher #"(?<=^)[\w\-\%]+(?=\=\?)" pattern)))

; #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&])"
; from
; "list"
(defn query-param-pattern [q-param-name]
  (re-pattern (str "(?<=[\\?\\&]" q-param-name "\\=)[\\w\\-\\%]+(?=[$\\&])")))

; [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&])"]
; from
; "list=?type"
(defn query-param-info [pattern]
  [(param-to-keyword pattern) (query-param-pattern (query-param-name pattern))])

; [[:offset #"(?<=[\?\&]offset\=)[\w\-\%]+(?=[$\&])"] [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&])"]]
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn query-info [pattern]
  (vec (map query-param-info (queryparams pattern))))

; "users"
; from
; "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"
; #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&])"
(defn get-query-param-with-info [uri pattern]
  (re-find (re-matcher pattern uri)))

; ==============================================================================
; === PATTERN INFO =============================================================

; {:host #"^(http|https)://dribbble\.com($|\/$|\/.+)", :path [[0 :user] [1 nil] [2 :id]], :query [[:offset #"(?<=[\?\&]offset\=)[\w\-\%]+(?=[$\&])"] [:type #"(?<=[\?\&]list\=)[\w\-\%]+(?=[$\&])"]]} 
; from
; "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type);"
(defn pattern-info [pattern]
  { :host (host-info pattern)
    :path (path-info pattern)
    :query (query-info pattern)})

