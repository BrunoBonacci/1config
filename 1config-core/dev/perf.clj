(ns perf
  (:require [com.brunobonacci.oneconfig :refer :all]
            [criterium.core :refer [bench quick-bench]]))


(comment

  ;; perf tests of the public APIs as of v0.8.0

  (quick-bench (one-config))
  ;; Evaluation count : 2664 in 6 samples of 444 calls.
  ;; Execution time mean : 226.795933 µs
  ;; Execution time std-deviation : 7.872293 µs
  ;; Execution time lower quantile : 221.213640 µs ( 2.5%)
  ;; Execution time upper quantile : 239.728521 µs (97.5%)
  ;; Overhead used : 3.726905 ns
  ;;
  ;; Found 1 outliers in 6 samples (16.6667 %)
  ;; low-severe	 1 (16.6667 %)
  ;; Variance from outliers : 13.8889 % Variance is moderately inflated by outliers


  ;; with end-2-end resolution in DynamoDB table with 10 read units provisioned
  (quick-bench (configure {:key "system1" :env "dev" :version "1.2.3-SNAPSHOT"}))
  ;;  Evaluation count : 6 in 6 samples of 1 calls.
  ;;  Execution time mean : 182.841613 ms
  ;;  Execution time std-deviation : 7.517325 ms
  ;;  Execution time lower quantile : 174.502340 ms ( 2.5%)
  ;;  Execution time upper quantile : 190.740907 ms (97.5%)
  ;;  Overhead used : 3.726905 ns



  (quick-bench
   (deep-merge {:a {:b {:c 1} :d 2} :e 3}
               {:a {:b {:c 10} :d 2} :z 5}))
  ;; Evaluation count : 170370 in 6 samples of 28395 calls.
  ;; Execution time mean : 3.666751 µs
  ;; Execution time std-deviation : 190.476455 ns
  ;; Execution time lower quantile : 3.441584 µs ( 2.5%)
  ;; Execution time upper quantile : 3.902907 µs (97.5%)
  ;; Overhead used : 2.100626 ns


  )
