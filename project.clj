(defproject dribbble-experiments "0.1.0-SNAPSHOT"
  :description "Dribble API experiments"
  :url "https://github.com/FI-Mihej/Dribbble-Experiments"
  :license {:name "GNU GENERAL PUBLIC LICENSE v.3"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [ [org.clojure/clojure "1.8.0"]
                  [clj-http "2.0.0"]
                  [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot dribbble-experiments.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
