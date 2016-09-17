(ns dribbble-experiments.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.string :as st]
            [clojure.data.json :as json]))

(def endpoint "https://api.dribbble.com/v1/")
(def test-user "louiethelowe")
(def test-access-token "")

(defonce app-state (atom {:requests-count 0}))

(defn dr-api [path token]
  (do
    (Thread/sleep 1000)
    (swap! app-state assoc :requests-count (inc (get (deref app-state) :requests-count)))
    (println (str "<< Request #" (:requests-count (deref app-state)) " >>"))
    (json/read-str (:body (client/get (st/join "" [endpoint path]) {:insecure? true
                                                                    :force-redirects true
                                                                    :headers {"Authorization" (st/join "" ["Bearer " token])}})))))

(defn username-and-name-by-user-id [user-id token]
  (let [user-info (dr-api (st/join "" ["users/" user-id]) token)]
    [(get user-info "username") (get user-info "name")]))

(defn followers-ids [user-id token]
  (let [user-followers (dr-api (st/join "" ["users/" user-id "/followers"]) token)]
    (vec (map (fn [follower-info] (get (get follower-info "follower") "id")) user-followers))))

(defn shots-ids [user-id token]
  (let [user-shots (dr-api (st/join "" ["users/" user-id "/shots"]) token)]
    (vec (map (fn [shot-info] (get shot-info "id")) user-shots))))

(defn shots-likers-ids [shot-id token]
  (let [shot-likers (dr-api (st/join "" ["shots/" shot-id "/likes"]) token)]
    (vec (map (fn [liker-info] (get (get liker-info "user") "id")) shot-likers))))

(defn find-top-likers-ids [user-name number-of-top-likers token]
  (let [user-info (dr-api (st/join "" ["users/" user-name]) token)
        user-id (get user-info "id")
        user-followers (followers-ids user-id token)
        all-shots-ids (flatten (vec (map (fn [follower-id] (shots-ids follower-id token)) user-followers)))
        all-likers-ids-flat (remove nil? (flatten (map (fn [shot-id] (shots-likers-ids shot-id token)) all-shots-ids)))
        likes-per-user-id (frequencies all-likers-ids-flat)
        top-likers-ids (take number-of-top-likers (sort-by val > likes-per-user-id))
        ]
    (println "")
    (println "User Info:")
    (println user-info)
    (println "")
    (println "Followers of the User:")
    (println user-followers)
    (println "")
    (println "Shots made by the Followers :")
    (println all-shots-ids)
    (println "")
    (println "User IDs of the all users who liked Shots:")
    (println all-likers-ids-flat)
    (println "")
    (println "Number of Likes per user ID of the all users who liked Shots:")
    (println likes-per-user-id)
    (println "")
    (println (str "User IDs and number of Likes of the Top " number-of-top-likers " Likers:"))
    (println top-likers-ids)
    top-likers-ids))

(defn find-top-likers-names [user-name number-of-top-likers token]
  (let [top-likers-ids (find-top-likers-ids user-name number-of-top-likers token)
        top-likers-names (vec (map (fn [user-id-count] (username-and-name-by-user-id (get user-id-count 0) token)) (seq top-likers-ids)))]
    (println "")
    (println "Names of the Top Likers:")
    (println top-likers-names)
    top-likers-names))

(defn print-top-likers [user-name number-of-top-likers token]
  (let [top-likers-names (find-top-likers-names user-name number-of-top-likers token)]
    (println "")
    (println "RESULT:")
    (println "Top likers (<username> - <name>):")
    (doseq  [liker top-likers-names] 
            (do
              (println (str "\"" (get liker 0) "\" - \"" (get liker 1) "\""))))
    ))

(defn -main
  [& args]
  (let [vec-args (vec args)
        args-number (count vec-args)
        user-name (if (<= 1 args-number) (get vec-args 0))
        access-token (if (<= 2 args-number) (get vec-args 1))]
    (if (= 1 args-number) (if (= "help" (get vec-args 0)) (do
        (println "")
        (println "Usage:")
        (println "lein run <Username> <AccessToken>")
        (println "")
        (println "Example:")
        (println "lein run \"louiethelowe\" \"a5c9da88ad44ffce697b62d3a264f648848ab2031eeea91814ec9b75e44b8320\"")
        )))
    (if  (<= 2 args-number) (print-top-likers user-name 10 access-token))
    (println "")
    (println (str "DONE. Made " (:requests-count (deref app-state)) " requests."))))
