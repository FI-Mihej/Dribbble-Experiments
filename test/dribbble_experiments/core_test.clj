(ns dribbble-experiments.core-test
  (:require [clojure.test :refer :all]
            [dribbble-experiments.core :refer :all]
            [dribbble-experiments.url_match :refer :all]))

(deftest url-match-tests
  (testing "URL-MATCH. Pattern Info."
    (is (=  (:fragment (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragmmmment(?paragraph)"))
            nil))
    (is (=  (:fragment (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(paragraph)"))
            nil))
    (is (=  (:fragment (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type)"))
            nil))
    (is (=  (:query (pattern-info "host(dribbble.com); path(?user/status/?id); querrrryparam(list=?type); fragment(?paragraph)"))
            nil))
    (is (=  (:query (pattern-info "host(dribbble.com); path(?user/status/?id); fragment(?paragraph)"))
            nil))
    (is (=  (:query (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(list=type); fragment(?paragraph)"))
            nil))
    (is (=  (:query (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(=?type); fragment(?paragraph)"))
            nil))
    (is (=  (:query (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(list=?); fragment(?paragraph)"))
            nil))
    (is (=  (:path (pattern-info "host(dribbble.com); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))
            nil))
    (is (=  (:path (pattern-info "host(dribbble.com); path(); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))
            nil))
    (is (not=  (:host (pattern-info "host(http://dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))
            nil))
    (is (not=  (:host (pattern-info "host(); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))
            nil))
    (is (not=  (:host (pattern-info "path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))
            nil))
  )
  (testing "URL-MATCH. Bad uri."
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://twitter.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/staaaaatus/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/status?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?liiiiiist=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/?list=users&offset=1&page=34")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "https://dribbble.com/")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "httttttttps://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")))
    (is (= nil (recognize 
                  (pattern-info "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)") 
                  "dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")))
  )
)
