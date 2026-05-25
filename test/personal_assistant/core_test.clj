(ns personal-assistant.core-test
  (:require [clojure.test :refer [deftest is]]
            [personal-assistant.core :as core]))

(deftest test-greet
  (is (= :ok (core/greet))))
