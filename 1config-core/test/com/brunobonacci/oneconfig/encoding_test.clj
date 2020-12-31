(ns com.brunobonacci.oneconfig.encoding-test
  (:refer-clojure :exclude [find load list])
  (:require  [midje.sweet :refer :all]
             [com.brunobonacci.oneconfig.backend :refer :all]
             [com.brunobonacci.oneconfig.backends.encoding
              :refer [make-encoding-wrapper]]
             [com.brunobonacci.oneconfig.backends.in-memory
              :refer [in-memory-config-backend TestStore data]]))



(defn rand-env [env]
  (str (name (gensym "testenv")) "-" env))



(defn rand-key [key]
  (str (name (gensym "key")) "-" key))



(facts "content-type: txt"

  (fact "can list items that have been saved"
    (let [store0 (in-memory-config-backend)
          store (make-encoding-wrapper store0)
          entry1 {:env          (rand-env "prod")
                  :key          (rand-key "service1")
                  :version      "1.2.3"
                  :content-type "txt"
                  :value        "some-value"}

          entry2  {:env          (rand-env "prod")
                   :key          (rand-key "service1")
                   :version      "1.2.4"
                   :content-type "txt"
                   :value        "some-value"}]
      (-> store
        (save entry1)
        (save entry2)
        (list {}))
      => (contains [(contains (dissoc entry1 :content-type :value))
                    (contains (dissoc entry2 :content-type :value))])))

  (fact "save and read a value a string "
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "txt"
                 :value "some-value"}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value) => "some-value"))


  (fact "save and read a numeric value should be returned as a string"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "txt"
                 :value 23}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => "23")


  (fact "anything else should fail."
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "txt"
                 :value {:some "complex" :value #{1}}}]
      (-> store
        (save entry)
        ))=> (throws Exception))
  )



(facts "content-type: edn"

  (fact "save and read a value a string "
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "edn"
                 :value "some-value"}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value) => "some-value"))



  (fact "save and read a numeric value"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "edn"
                 :value 23}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => 23)


  (fact "save and read a data-structure"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "edn"
                 :value {:some "complex" :value #{1}}}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => {:some "complex" :value #{1}})
  )



(facts "content-type: json"

  (fact "save and read a value a string "
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "json"
                 :value "some-value"}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value) => "some-value"))


  (fact "save and read a numeric value"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "json"
                 :value 23}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => 23)


  (fact "save and read a data-structure"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "json"
                 :value {:some "complex" :value #{1}}}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => {:some "complex" :value [1]})
  )



(facts "content-type: yaml"

  (fact "save and read a value a string "
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "yaml"
                 :value "some-value"}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value) => "some-value"))


  (fact "save and read a numeric value"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "yaml"
                 :value 23}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => 23)


  (fact "save and read a data-structure"
    (let [store0 (in-memory-config-backend)
          store  (make-encoding-wrapper store0)
          entry {:env (rand-env "prod")
                 :key (rand-key "service1")
                 :version "1.2.3"
                 :content-type "yaml"
                 :value {:some "complex" :values [1 2 3 4] :maps {:a 1 :b 2}}}]
      (-> store
        (save entry)
        (load (dissoc entry :value :content-type))
        :value)) => {:some "complex" :values [1 2 3 4] :maps {:a 1 :b 2}})
  )
