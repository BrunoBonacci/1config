(ns perf
  (:require [com.brunobonacci.oneconfig-core :refer :all]
            [criterium.core :refer [bench quick-bench]]))


(comment

  ;; perf tests

  (bench (Thread/sleep 1000))

  )
