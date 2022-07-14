(ns com.brunobonacci.oneconfig.util-test
  (:require [com.brunobonacci.oneconfig.util :as ut]
            [midje.sweet :refer [fact]]))



(fact "You can override the 1config home directory with the `1config.home` property"
  (def dir "/tmp")
  (System/setProperty "1config.home" dir)
  (ut/home-1config) => dir
  (System/clearProperty "1config.home")
  )
