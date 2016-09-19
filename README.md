# dribbble-experiments

1) Experiments with Dribbble API
2) URI parsing and match library

## Build

    $ lein uberjar

## Run

Usage:

    lein run <Username> <AccessToken>

Example:

    $ lein run "louiethelowe" "960d368303253732c7d0d6e2081c5ab8efe1e79349a223ce4d8458be4e49948c"

Help:

    $ lein run help

## Jar Usage

Args are the same as in "Run" part

    $ java -jar dribbble-experiments-0.1.0-SNAPSHOT.jar [args]

## Run Tests for URI parsing and match library

    $ lein test

# URI parsing and match library

## Examples

```clojure
user=>(def pattern (pattern-info 
    "host(dribbble.com); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))

user=>(recognize 
    pattern 
    "https://dribbble.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")
[   [:user "some-username"] 
    [:id "1905065-Travel-Icons-pack"] 
    [:offset "1"] 
    [:type "users"] 
    [:paragraph "paragraph=3"]]

user=>(recognize 
    pattern 
    "https://twitter.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")
nil

user=>(def pattern-any-host (pattern-info 
    "host(); path(?user/status/?id); queryparam(offset=?offset); queryparam(list=?type); fragment(?paragraph)"))

user=>(recognize 
    pattern-any-host 
    "https://twitter.com/some-username/status/1905065-Travel-Icons-pack?list=users&offset=1&page=34#paragraph=3")
[   [:user "some-username"] 
    [:id "1905065-Travel-Icons-pack"] 
    [:offset "1"] 
    [:type "users"] 
    [:paragraph "paragraph=3"]]
```
    