(ns personal-assistant.memory-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [personal-assistant.memory :as memory]))

(deftest load-user-profile-existing
  (testing "returns file content when file exists"
    (let [tmp (java.io.File/createTempFile "user" ".md")]
      (try
        (spit tmp "Name: Ada\nRole: Engineer")
        (is (= "Name: Ada\nRole: Engineer" (memory/load-user-profile (.getPath tmp))))
        (finally (.delete tmp))))))

(deftest load-user-profile-missing
  (testing "returns empty string when file is absent"
    (is (= "" (memory/load-user-profile "/nonexistent/path/USER.md")))))

(deftest load-memory-existing
  (testing "returns file content when file exists"
    (let [tmp (java.io.File/createTempFile "memory" ".md")]
      (try
        (spit tmp "- Likes coffee\n- Prefers dark mode")
        (is (= "- Likes coffee\n- Prefers dark mode" (memory/load-memory (.getPath tmp))))
        (finally (.delete tmp))))))

(deftest load-memory-missing
  (testing "returns empty string when file is absent"
    (is (= "" (memory/load-memory "/nonexistent/path/MEMORY.md")))))

(deftest build-memory-context-both
  (testing "includes both sections when both are non-empty"
    (let [ctx (memory/build-memory-context "Name: Ada" "- Likes coffee")]
      (is (str/includes? ctx "## User Profile"))
      (is (str/includes? ctx "Name: Ada"))
      (is (str/includes? ctx "## Memory"))
      (is (str/includes? ctx "- Likes coffee")))))

(deftest build-memory-context-empty-profile
  (testing "omits User Profile section when profile is blank"
    (let [ctx (memory/build-memory-context "" "- Likes coffee")]
      (is (not (str/includes? ctx "## User Profile")))
      (is (str/includes? ctx "## Memory")))))

(deftest build-memory-context-empty-memory
  (testing "omits Memory section when memory is blank"
    (let [ctx (memory/build-memory-context "Name: Ada" "")]
      (is (str/includes? ctx "## User Profile"))
      (is (not (str/includes? ctx "## Memory"))))))

(deftest build-memory-context-both-empty
  (testing "returns empty string when both are blank"
    (is (= "" (memory/build-memory-context "" "")))))

(deftest extract-new-facts-zero
  (testing "returns empty seq when no markers present"
    (is (empty? (memory/extract-new-facts "Hello, how are you?")))))

(deftest extract-new-facts-one
  (testing "returns one fact string"
    (is (= ["User prefers dark mode"]
           (memory/extract-new-facts "Sure! <!-- remember: User prefers dark mode --> Done.")))))

(deftest extract-new-facts-multiple
  (testing "returns all fact strings in order"
    (let [response "<!-- remember: Fact one --> text <!-- remember: Fact two -->"]
      (is (= ["Fact one" "Fact two"] (memory/extract-new-facts response))))))

(deftest strip-markers-removes-all
  (testing "strips markers and leaves surrounding text trimmed"
    (is (= "Hello. Done." (memory/strip-markers "Hello. <!-- remember: some fact --> Done.")))))

(deftest strip-markers-no-markers
  (testing "returns text unchanged when no markers present"
    (is (= "Plain text." (memory/strip-markers "Plain text.")))))

(deftest append-facts-to-daily-note-creates-and-writes
  (testing "creates file and writes facts as markdown list items"
    (let [tmp-dir (doto (java.io.File/createTempFile "memory-base" nil)
                    (.delete)
                    (.mkdirs))]
      (try
        (memory/append-facts-to-daily-note ["Fact one" "Fact two"] "2026-05-31" (.getPath tmp-dir))
        (let [content (slurp (io/file tmp-dir "2026-05-31.md"))]
          (is (str/includes? content "- Fact one"))
          (is (str/includes? content "- Fact two")))
        (finally
          (doseq [f (reverse (file-seq tmp-dir))]
            (.delete f)))))))

(deftest append-facts-to-daily-note-appends
  (testing "appends to an existing file"
    (let [tmp-dir (doto (java.io.File/createTempFile "memory-base2" nil)
                    (.delete)
                    (.mkdirs))]
      (try
        (memory/append-facts-to-daily-note ["First"] "2026-05-31" (.getPath tmp-dir))
        (memory/append-facts-to-daily-note ["Second"] "2026-05-31" (.getPath tmp-dir))
        (let [content (slurp (io/file tmp-dir "2026-05-31.md"))]
          (is (str/includes? content "- First"))
          (is (str/includes? content "- Second")))
        (finally
          (doseq [f (reverse (file-seq tmp-dir))]
            (.delete f)))))))
