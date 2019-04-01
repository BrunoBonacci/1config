(ns com.brunobonacci.oneconfig.backend-test
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory :refer [in-memory-config-backend]]
            [com.brunobonacci.oneconfig.backends.dynamo :refer [dynamo-config-backend]]
            [midje.sweet :refer :all]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn rand-env [env]
  (str env "-" (uuid) ))

(defn rand-key [key]
  (str key "-" (uuid) ))

(defn compatibility-tests [store]

  (facts "config-backend: can store a value and read it back given a env, key and version"

         (let [entry {:env (rand-env "prod")
                      :key (rand-key "service1")
                      :version "1.2.3"
                      :value "some-config"}]
           (-> store
              (save entry)
              (load (dissoc entry :value)))
           => (contains entry)))


  (facts "config-backend: loading a key which doesn't exist returns nil"

         (fact "  > different environment"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     ver    "1.2.3"
                     entry1 {:env env
                             :key key
                             :version ver
                             :value "some-config1"}]
                 (-> store
                    (save entry1)
                    (load {:env (rand-env "dev")
                           :key key
                           :version ver}))
                 => nil))

         (fact "  > different key"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     ver    "1.2.3"
                     entry1 {:env env
                             :key key
                             :version ver
                             :value "some-config1"}]
                 (-> store
                    (save entry1)
                    (load {:env env
                           :key (rand-key "service2")
                           :version ver}))
                 => nil))
         )


  (facts "config-backend: storing multiple updates for the same env, key and version, only return last one"

         (let [env    (rand-env "prod")
               key    (rand-key "service1")
               ver    "1.2.3"
               entry1 {:env env
                       :key key
                       :version ver
                       :value "some-config1"}
               entry2 {:env env
                       :key key
                       :version ver
                       :value "some-config2"}
               entry3 {:env env
                       :key key
                       :version ver
                       :value "some-config3"}
               ]
           (-> store
              (save entry1)
              (save entry2)
              (save entry3)
              (load {:env env
                     :key key
                     :version ver}))
           => (contains entry3)))


  (facts "config-backend: you can load a specific change-num if it exists"

         (fact "  > the change is returned when it exists"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     ver    "1.2.3"
                     entry1 {:env env
                             :key key
                             :version ver
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version ver
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version ver
                             :value "some-config3"}
                     store2 (-> store
                               (save entry1)
                               (save entry2))

                     cnum (:change-num
                           (load store2 {:env env
                                         :key key
                                         :version ver}))

                     store3 (save store2 entry3)]

                 (load store3 {:env env
                               :key key
                               :version ver
                               :change-num cnum})
                 => (contains entry2)))


         (fact "  > nil is returned when it doesn't exists"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     ver    "1.2.3"
                     entry1 {:env env
                             :key key
                             :version ver
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version ver
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version ver
                             :value "some-config3"}
                     ]
                 (-> store
                    (save entry1)
                    (save entry2)
                    (save entry3)
                    (load {:env env
                           :key key
                           :version ver
                           :change-num 10}))
                 => nil))
         )



  (facts "config-backend: `load` returns the exact version when found or nil"

         (fact "  > the specific version is returned when it exists"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     entry1 {:env env
                             :key key
                             :version "1.2.3"
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version "1.2.5"
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version "1.3.0"
                             :value "some-config3"}
                     ]
                 (-> store
                    (save entry1)
                    (save entry2)
                    (save entry3)
                    (load {:env env
                           :key key
                           :version "1.2.5"}))
                 => (contains entry2)))


         (fact "  > nil is returned when it doesn't exists"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     entry1 {:env env
                             :key key
                             :version "1.2.3"
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version "1.2.5"
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version "1.3.0"
                             :value "some-config3"}
                     ]
                 (-> store
                    (save entry1)
                    (save entry2)
                    (save entry3)
                    (load {:env env
                           :key key
                           :version "1.2.7"}))
                 => nil))
         )


  (facts "config-backend: `list` returns all enties that match filters and sorted according to :key :env :version :change-num"

         (let [env1 (rand-env "prod")
               env2 (rand-env "dev")

               key1 (rand-key "serviceb")
               key2 (rand-key "servicea")
               entry1 {:env     env1
                       :key     key1
                       :version "1.2.3"
                       :value   "some-config1"}
               entry2 {:env     env1
                       :key     key1
                       :version "1.2.3"
                       :value   "some-config2"}
               entry3 {:env     env1
                       :key     key1
                       :version "1.2.5"
                       :value   "some-config3"}
               entry4 {:env     env2
                       :key     key1
                       :version "1.3.0"
                       :value   "some-config4"}

               entry5 {:env     env1
                       :key     key2
                       :version "1.3.0"
                       :value   "some-config5"}

               entry6 {:env     env1
                       :key     key1
                       :version "1.22.5"
                       :value   "some-config3"}

               entry7 {:env     env1
                       :key     key1
                       :version "1.3.5"
                       :value   "some-config3"}


               store (-> store
                        (save entry2)
                        (save entry1)
                        (save entry3)
                        (save entry4)
                        (save entry5)
                        (save entry6)
                        (save entry7))]

           (fact "env filter returns entries that match"
                 (->> (list store {:env env1})
                    (map #(dissoc % :change-num :content-type :backend)))


                 => [(dissoc entry5 :value)
                    (dissoc entry2 :value)
                    (dissoc entry1 :value)
                    (dissoc entry3 :value)
                    (dissoc entry7 :value)
                    (dissoc entry6 :value)])


           (fact "key filter returns entries that match"
                 (->> (list store {:key key2})
                    (map #(dissoc % :change-num :content-type)))
                 => (contains [(contains (dissoc entry5 :value))]))


           (fact "version filter returns entries that match and sorted by order inserted"
                 (->> (list store {:version "1.2.3"})
                    (map #(dissoc % :change-num :content-type)))
                 => (contains [(contains (dissoc entry2 :value)) (contains (dissoc entry1 :value))]))

           (fact "Can combine filters"
                 (->> (list store {:version "1.3.0" :env env2})
                    (map #(dissoc % :change-num :content-type)))
                 => (contains [(contains (dissoc entry4 :value)) ])))


         (fact "  > nil is returned when it doesn't exists"
               (let [env (rand-env "prod")
                     key (rand-key "service1")
                     entry1 {:env     env
                             :key     key
                             :version "1.2.3"
                             :value   "some-config1"}
                     entry2 {:env     env
                             :key     key
                             :version "1.2.5"
                             :value   "some-config2"}
                     entry3 {:env     env
                             :key     key
                             :version "1.3.0"
                             :value   "some-config3"}
                     ]
                 (-> store
                    (save entry1)
                    (save entry2)
                    (save entry3)
                    (load {:env     env
                           :key     key
                           :version "1.2.7"}))
                 => nil))
         )



  (facts "config-backend: `find` returns a less-or-equal version (in semantic version terms) or nil of not found "

         (fact "  > the specific version is returned when it exists"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     entry1 {:env env
                             :key key
                             :version "1.2.3"
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version "1.2.5"
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version "1.3.0"
                             :value "some-config3"}
                     ]
                 (-> store
                    (save entry1)
                    (save entry2)
                    (save entry3)
                    (find {:env env
                           :key key
                           :version "1.2.5"}))
                 => (contains entry2)))


         (fact "  > the less-or-equal version is returned when exact match is not found"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     entry1 {:env env
                             :key key
                             :version "1.2.3"
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version "1.2.5"
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version "1.3.0"
                             :value "some-config3"}
                     ]
                 (-> store
                     (save entry1)
                     (save entry2)
                     (save entry3)
                     (find {:env env
                            :key key
                            :version "1.2.7"}))
                 => (contains entry2)))


         (fact "  > nil is returned when it doesn't exists"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     entry1 {:env env
                             :key key
                             :version "1.2.3"
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version "1.2.5"
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version "1.3.0"
                             :value "some-config3"}
                     ]
                 (-> store
                     (save entry1)
                     (save entry2)
                     (save entry3)
                     (find {:env env
                            :key key
                            :version "1.1.7"}))
                 => nil))


         (fact "  > semantic version comparison should be used rather than lexicographic"
               (let [env    (rand-env "prod")
                     key    (rand-key "service1")
                     entry1 {:env env
                             :key key
                             :version "1.1.5"
                             :value "some-config1"}
                     entry2 {:env env
                             :key key
                             :version "1.3.0"
                             :value "some-config2"}
                     entry3 {:env env
                             :key key
                             :version "1.10.7"
                             :value "some-config3"}
                     ]
                 (-> store
                     (save entry1)
                     (save entry2)
                     (save entry3)
                     (find {:env env
                            :key key
                            :version "1.2.0"}))
                 => (contains entry1)))
         )
  )


(facts "(*) compatibility tests for: in-memory-config-backend"

       (compatibility-tests (in-memory-config-backend)))


(facts "(*) compatibility tests for: dynamo-config-backend" :integration

       ;; to run locally/repl requires
       ;; (System/setProperty "aws.accessKeyId" "xxx")
       ;; (System/setProperty "aws.secretKey"   "xxx")

       (compatibility-tests (dynamo-config-backend {:table "1ConfigTest" :endpoint "eu-west-1"})))
