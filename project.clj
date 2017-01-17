(defproject diginetmedia/exif-processor "0.2.0-SNAPSHOT"
  :description "A lightweight Clojure wrapper around the exif processing facility in Drew Oakes' metadata-extractor"
  :url "http://github.com/ninerian/exif-processor"
  :min-lein-version "2.1.0"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.drewnoakes/metadata-extractor "2.9.1"]
                 [clj-http "2.3.0"]]
  :profiles {:dev {:resource-paths ["resources"]}}
  :aliases {"release" ["deploy" "clojars"]})
