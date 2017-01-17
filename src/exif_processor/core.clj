(ns exif-processor.core
  (:use [clojure.string :only [join]])
  (:require [clj-http.client :as client])
  (:import [java.io BufferedInputStream FileInputStream]
           [com.drew.imaging ImageMetadataReader]
           [com.drew.metadata.xmp XmpDirectory]))

(def exif-directory-regex
  (re-pattern (str "(?i)(" (join "|"
                                 ["Exif" "JPEG" "JFIF" "Photoshop"
                                  "Agfa" "Canon" "Casio" "Epson"
                                  "Fujifilm" "Kodak" "Kyocera"
                                  "Leica" "Minolta" "Nikon" "Olympus"
                                  "Panasonic" "Pentax" "Sanyo"
                                  "Sigma/Foveon" "Sony"]) ")")))

(defn- get-metadata
  "Get metadata for a File."
  [^java.io.File file]
  (-> file
      (ImageMetadataReader/readMetadata)))


(defn- fields->map
  "Convert field entries to a clojure map."
  [tag]
  (into {} (map #(hash-map (.getTagName %) (.getDescription %)) tag)))


(defn- filter-xmp
	"Filters a seq of the XMP metadata by the given string"
  [seq filter-str]
  (let [pattern (re-pattern (str filter-str))]
    (->> seq
         (filter #(not (nil? (.getPath %))))
         (filter #(re-find pattern (.toLowerCase (.getPath %)))))))

(defn get-xmp-data
	"Returns the XMP directory of the meta data as map. Could be filtered by a given string"
  [metadata filter]
  (let [xmp (.getFirstDirectoryOfType metadata (class (XmpDirectory.)))
        xmp-meta 	(.getXMPMeta xmp)
        data-seq (iterator-seq (.iterator xmp-meta))
        filtered-data (filter-xmp data-seq filter)]
    (into {}	(map #(hash-map (.getPath %)(.getValue %)) filtered-data))))

(defn directories->map
  "Generate a clojure map of directories/data from metadata."
  [metadata]
  (->> metadata
       .getDirectories
       (filter #(re-find exif-directory-regex (.getName %)))
       (map (fn [directory]
              [(.getName directory)
               (-> directory
                   .getTags
                   fields->map)]))
       (into {})))

(defn exif->map
  "Generate a clojure map of the metadata."
  [metadata]
  (->> metadata
       .getDirectories
       (filter #(re-find exif-directory-regex (.getName %)))
       (map #(.getTags %))
       (map fields->map)
       (into {})))


(defn exif-for-file
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [file]
  (let [metadata (get-metadata file)
        data (exif->map metadata)
        xmp (get-xmp-data metadata "gpano")]
    (merge data xmp)))

(defn exif-for-file-struct
  [file]
  (let [metadata (get-metadata file)
        data (directories->map metadata)
        xmp (get-xmp-data metadata "gpano")]
    (merge data {"XMP" xmp})))

(defn exif-for-filename
  "Loads a file from a give filename and extracts exif information into a map"
  [filename]
  (exif-for-file (FileInputStream. filename)))

(defn exif-for-url
  "Streams a file from a given URL and extracts exif information into a map"
  [url]
  (exif-for-file (BufferedInputStream. (:body (client/get url {:as :stream})))))
