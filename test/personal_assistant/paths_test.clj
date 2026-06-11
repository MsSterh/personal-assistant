(ns personal-assistant.paths-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [personal-assistant.paths :as paths])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir ^File []
  (.toFile (Files/createTempDirectory "paths-test" (make-array FileAttribute 0))))

(deftest traversal-rejected
  (testing "../ traversal out of CWD is rejected"
    (let [root (.getCanonicalFile (temp-dir))
          result (paths/validate-write-path "../escape.txt" root)]
      (is (:error result))
      (is (nil? (:path result))))))

(deftest absolute-outside-rejected
  (testing "absolute path outside CWD is rejected"
    (let [root (.getCanonicalFile (temp-dir))
          result (paths/validate-write-path "/tmp/definitely-outside.txt" root)]
      (is (:error result)))))

(deftest symlink-escape-rejected
  (testing "symlink pointing outside CWD is rejected via real-path check"
    (let [root (.getCanonicalFile (temp-dir))
          outside (.getCanonicalFile (temp-dir))
          link (io/file root "link")]
      (Files/createSymbolicLink (.toPath link) (.toPath outside)
                                (make-array FileAttribute 0))
      (let [result (paths/validate-write-path "link/inside.txt" root)]
        (is (:error result))))))

(deftest valid-in-cwd-accepted
  (testing "valid in-CWD path (including nested) is accepted"
    (let [root (.getCanonicalFile (temp-dir))]
      (is (:path (paths/validate-write-path "file.txt" root)))
      (is (:path (paths/validate-write-path "nested/dir/file.txt" root)))
      (is (nil? (:error (paths/validate-write-path "nested/dir/file.txt" root)))))))

(deftest existing-nested-accepted
  (testing "existing nested file resolves and is accepted"
    (let [root (.getCanonicalFile (temp-dir))
          nested (io/file root "a" "b")]
      (.mkdirs nested)
      (spit (io/file nested "f.txt") "hi")
      (is (:path (paths/validate-write-path "a/b/f.txt" root))))))
