(ns perf
  (:require [com.brunobonacci.oneconfig :refer :all]
            [criterium.core :refer [bench quick-bench]]))


(comment

  ;; perf tests of the public APIs as of v0.8.0 (JDK 12)

  ;; with end-2-end resolution in DynamoDB table with 10 read units
  ;; provisioned (it includes network transit (outside of AWS) and
  ;; Dynamo query, key lookup, decryption, payload parsing.
  (quick-bench
   (configure
    {:key "user-profile" :env "uat" :version "1.2.3-SNAPSHOT"}))

  ;; Evaluation count : 6 in 6 samples of 1 calls.
  ;; Execution time mean : 112.783515 ms
  ;; Execution time std-deviation : 1.944561 ms
  ;; Execution time lower quantile : 110.490900 ms ( 2.5%)
  ;; Execution time upper quantile : 114.737041 ms (97.5%)
  ;; Overhead used : 1.792913 ns



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
