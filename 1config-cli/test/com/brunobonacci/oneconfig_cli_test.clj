(ns com.brunobonacci.oneconfig-cli-test
  (:require [com.brunobonacci.oneconfig-cli :refer :all]
            [midje.sweet :refer :all]))


(fact "is it cool?"
      (foo) => "do something cool")
