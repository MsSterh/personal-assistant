(ns personal-assistant.tools.files-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [personal-assistant.tools.files :as files])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:dynamic *cwd* nil)

(defn- temp-dir ^File []
  (.toFile (Files/createTempDirectory "files-test" (make-array FileAttribute 0))))

;; Run each test with `user.dir` pointed at a fresh temp dir so the CWD-only
;; containment checks operate on a sandbox we can freely mutate.
(use-fixtures :each
  (fn [t]
    (let [dir (.getCanonicalFile (temp-dir))
          prev (System/getProperty "user.dir")]
      (System/setProperty "user.dir" (.getPath dir))
      (try
        (binding [*cwd* dir] (t))
        (finally
          (System/setProperty "user.dir" prev))))))

(deftest read-out-of-cwd
  (testing "read of an out-of-CWD file returns its contents"
    (let [outside (.getCanonicalFile (temp-dir))
          f (io/file outside "note.txt")]
      (spit f "hello outside")
      (let [result (files/read-file {:path (.getPath f)})]
        (is (= "hello outside" (:content result)))
        (is (nil? (:error result)))))))

(deftest write-inside-cwd
  (testing "write inside CWD creates the file"
    (let [result (files/write-file {:path "out.txt" :content "data"})]
      (is (nil? (:error result)))
      (is (= "data" (slurp (io/file *cwd* "out.txt")))))))

(deftest overwrite-inside-cwd
  (testing "write overwrites an existing file"
    (spit (io/file *cwd* "out.txt") "old")
    (files/write-file {:path "out.txt" :content "new"})
    (is (= "new" (slurp (io/file *cwd* "out.txt"))))))

(deftest append-creates-file
  (testing "append creates the file when absent"
    (let [result (files/append-file {:path "log.txt" :content "line1\n"})]
      (is (nil? (:error result)))
      (is (= "line1\n" (slurp (io/file *cwd* "log.txt")))))))

(deftest append-adds-to-end
  (testing "append adds content to the end of an existing file"
    (spit (io/file *cwd* "log.txt") "line1\n")
    (files/append-file {:path "log.txt" :content "line2\n"})
    (is (= "line1\nline2\n" (slurp (io/file *cwd* "log.txt"))))))

(deftest append-outside-refused
  (testing "append outside CWD is refused and no file is created"
    (let [outside (.getCanonicalFile (temp-dir))
          target (io/file outside "blocked.txt")
          result (files/append-file {:path (.getPath target) :content "x"})]
      (is (:error result))
      (is (not (.exists target))))))

(deftest write-outside-refused
  (testing "write outside CWD is refused and no file is created"
    (let [outside (.getCanonicalFile (temp-dir))
          target (io/file outside "blocked.txt")
          result (files/write-file {:path (.getPath target) :content "x"})]
      (is (:error result))
      (is (not (.exists target))))))

(deftest edit-exact-match
  (testing "edit replaces an exact match"
    (spit (io/file *cwd* "e.txt") "foo bar baz")
    (let [result (files/edit-file {:path "e.txt" :old-string "bar" :new-string "QUX"})]
      (is (= 1 (:replaced result)))
      (is (= "foo QUX baz" (slurp (io/file *cwd* "e.txt")))))))

(deftest edit-missing-match
  (testing "edit with missing match returns an error"
    (spit (io/file *cwd* "e.txt") "foo bar")
    (is (:error (files/edit-file {:path "e.txt" :old-string "nope" :new-string "x"})))))

(deftest edit-non-unique-match
  (testing "edit with non-unique match returns an error"
    (spit (io/file *cwd* "e.txt") "x x")
    (is (:error (files/edit-file {:path "e.txt" :old-string "x" :new-string "y"})))))

(deftest delete-inside-cwd
  (testing "delete succeeds inside CWD"
    (let [f (io/file *cwd* "d.txt")]
      (spit f "bye")
      (is (:deleted (files/delete-file {:path "d.txt"})))
      (is (not (.exists f))))))

(deftest delete-outside-refused
  (testing "delete outside CWD is refused"
    (let [outside (.getCanonicalFile (temp-dir))
          f (io/file outside "keep.txt")]
      (spit f "stay")
      (is (:error (files/delete-file {:path (.getPath f)})))
      (is (.exists f)))))

(deftest move-inside-cwd
  (testing "move succeeds inside CWD"
    (spit (io/file *cwd* "src.txt") "content")
    (let [result (files/move-file {:source "src.txt" :dest "dst.txt"})]
      (is (:moved result))
      (is (not (.exists (io/file *cwd* "src.txt"))))
      (is (= "content" (slurp (io/file *cwd* "dst.txt")))))))

(deftest move-outside-refused
  (testing "move with destination outside CWD is refused"
    (spit (io/file *cwd* "src.txt") "content")
    (let [outside (.getCanonicalFile (temp-dir))
          dst (io/file outside "out.txt")
          result (files/move-file {:source "src.txt" :dest (.getPath dst)})]
      (is (:error result))
      (is (not (.exists dst)))
      (is (.exists (io/file *cwd* "src.txt"))))))

(deftest list-dir-entries
  (testing "list-dir returns expected entries"
    (spit (io/file *cwd* "a.txt") "1")
    (.mkdirs (io/file *cwd* "sub"))
    (let [result (files/list-dir {:path "."})
          names (set (map :name (:entries result)))]
      (is (contains? names "a.txt"))
      (is (contains? names "sub"))
      (is (true? (:dir? (first (filter #(= "sub" (:name %)) (:entries result)))))))))
