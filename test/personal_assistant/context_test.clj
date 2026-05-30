(ns personal-assistant.context-test
  (:require [clojure.test :refer [deftest is testing]]
            [personal-assistant.context :as context]))

(deftest load-soul-from-temp-file
  (testing "load-soul parses YAML frontmatter and returns system message"
    (let [tmp (java.io.File/createTempFile "soul" ".md")]
      (try
        (spit tmp "---\nname: Aria\nsex: female\nbehavior: You are helpful.\n---\n")
        (let [soul (context/load-soul tmp)]
          (is (= "system" (:role soul)))
          (is (= "You are helpful." (:content soul))))
        (finally (.delete tmp))))))

(deftest load-soul-fallback
  (testing "load-soul uses raw content when no frontmatter present"
    (let [tmp (java.io.File/createTempFile "soul" ".md")]
      (try
        (spit tmp "Plain system prompt.")
        (let [soul (context/load-soul tmp)]
          (is (= "system" (:role soul)))
          (is (= "Plain system prompt." (:content soul))))
        (finally (.delete tmp))))))
