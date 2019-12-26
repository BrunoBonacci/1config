(ns demo-data
  (:require [com.brunobonacci.oneconfig :refer :all]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends :as b]
            [com.brunobonacci.oneconfig.util :as u]
            [cheshire.core :as json]
            [amazonica.aws.dynamodbv2 :as dyn]))


(defn to-json
  [v]
  (json/generate-string v {:pretty true}))



(defn clear-table
  []
  (loop [items (:items (dyn/scan {:table-name "1Config"}))]
    (doseq [i items]
      (dyn/delete-item {:table-name "1Config"
                        :key (select-keys i [:__sys_key :__ver_key])}))
    (when (seq items)
      (recur (:items (dyn/scan {:table-name "1Config"}))))))

(def backend
  (b/backend-factory {:type :default}))


(def user-service
  [{:key "user-service"
    :version "0.1.2"
    :env "dev"
    :content-type "edn"
    :encoded true
    :value "
{:db
  {:host \"127.0.0.1\" :port 1234
   :user \"db-user\" :pass \"SupErS3crEt\"}}"}


   {:key "user-service"
    :version "0.2.0"
    :env "dev"
    :content-type "edn"
    :encoded true
    :value "
{:db
  {:host \"127.0.0.1\" :port 1234
   :user \"db-user\" :pass \"SupErS3crEt\"}
  ;; increase the number of pass retries
  :max-pass-retries 10}"}


   {:key "user-service"
    :version "0.5.0"
    :env "dev"
    :content-type "edn"
    :encoded true
    :value "
{:account-db {:host \"127.0.0.1\" :port 1234 :user \"db-user\" :pass \"SupErS3crEt\"}
 :timeline-db {:host \"10.10.0.1\" :port 4321 :user \"db-user\" :pass \"T1meline!\"}
 ;; increase the number of pass retries
 :max-pass-retries 10}"}


   {:key "user-service"
    :version "1.0.0"
    :env "dev"
    :content-type "edn"
    :encoded true
    :value "
{:account-db {:host \"127.0.0.1\" :port 1234 :user \"db-user\" :pass \"SupErS3crEt\"}
 :timeline-db {:host \"10.10.0.1\" :port 4321 :user \"db-user\" :pass \"T1meline!\"}
 ;; increase the number of pass retries
 :max-pass-retries 10}"}
   ])



(def notification-service
  [{:key "notification-service"
    :version "0.1.2"
    :env "dev"
    :content-type "json"
    :encoded true
    :value
    (to-json
     {:backends [{:mail :ses}
                 {:sms  :twilio :token "asdfghjkl3245678"}
                 {:notification :ses}]})
    }


   {:key "notification-service"
    :version "0.2.0"
    :env "uat"
    :content-type "json"
    :encoded true
    :value
    (to-json
     {:backends [{:mail :ses}
                 {:sms  :twilio :token "jhasgdf23487werg"}
                 {:notification :ses}]})}


   {:key "notification-service"
    :version "1.0.0"
    :env "prod"
    :content-type "json"
    :encoded true
    :value
    (to-json
     {:backends [{:mail :ses}
                 {:sms  :twilio :token "plojniugtadfgwe5"}
                 {:notification :ses}]})}

   ])





(def database
  [{:key "mysql-database"
    :version "5.1.0"
    :env "uat"
    :content-type "txt"
    :value "
# Example MySQL config file for small systems.
#
# This is for a system with little memory (<= 64M) where MySQL is only used
# from time to time and it's important that the mysqld daemon
# doesn't use much resources.
#
# You can copy this file to
# /etc/my.cnf to set global options,
# mysql-data-dir/my.cnf to set server-specific options (in this
# installation this directory is /var/lib/mysql) or
# ~/.my.cnf to set user-specific options.
#
# In this file, you can use all long options that a program supports.
# If you want to know which options a program supports, run the program
# with the --help option.

# The following options will be passed to all MySQL clients
[client]
password    = SupErS3crEt
port        = 3306
socket        = /var/run/mysqld/mysqld.sock

# Here follows entries for some specific programs

# The MySQL server
[mysqld]
port        = 3306
socket        = /var/run/mysqld/mysqld.sock
skip-locking
key_buffer_size = 16K
max_allowed_packet = 1M
table_open_cache = 4
sort_buffer_size = 64K
read_buffer_size = 256K
read_rnd_buffer_size = 256K
net_buffer_length = 2K
thread_stack = 128K
"}


   {:key "mysql-database"
    :version "5.1.0"
    :env "prod"
    :content-type "txt"
    :value "
# Example MySQL config file for small systems.
#
# This is for a system with little memory (<= 64M) where MySQL is only used
# from time to time and it's important that the mysqld daemon
# doesn't use much resources.
#
# You can copy this file to
# /etc/my.cnf to set global options,
# mysql-data-dir/my.cnf to set server-specific options (in this
# installation this directory is /var/lib/mysql) or
# ~/.my.cnf to set user-specific options.
#
# In this file, you can use all long options that a program supports.
# If you want to know which options a program supports, run the program
# with the --help option.

# The following options will be passed to all MySQL clients
[client]
password    = SupErS3crEt
port        = 3306
socket        = /var/run/mysqld/mysqld.sock

# Here follows entries for some specific programs

# The MySQL server
[mysqld]
port        = 3306
socket        = /var/run/mysqld/mysqld.sock
skip-locking
key_buffer_size = 16K
max_allowed_packet = 1M
table_open_cache = 4
sort_buffer_size = 64K
read_buffer_size = 256K
read_rnd_buffer_size = 256K
net_buffer_length = 2K
thread_stack = 128K
"}

   ])


(def mailer-service
  [{:key "mailer-service"
    :version "0.1.2"
    :env "dev"
    :content-type "properties"
    :encoded true
    :value
    "
# Simple properties
mail.hostname=mailer@mail.com
mail.port=9000
mail.from=mailer@mail.com

# List properties
mail.defaultRecipients[0]=admin@mail.com
mail.defaultRecipients[1]=owner@mail.com

# Map Properties
mail.additionalHeaders.redelivery=true
mail.additionalHeaders.secure=true

# Object properties
mail.credentials.username=mailer
mail.credentials.password=password
mail.credentials.authMethod=SHA1
"
    }


   {:key "mailer-service"
    :version "1.6.1"
    :env "dev"
    :content-type "properties"
    :encoded true
    :value
    "
# Simple properties
mail.hostname=mailer@mail.com
mail.port=9000
mail.from=mailer@mail.com

# List properties
mail.defaultRecipients[0]=admin@mail.com
mail.defaultRecipients[1]=owner@mail.com

# Map Properties
mail.additionalHeaders.redelivery=true
mail.additionalHeaders.secure=true

# Object properties
mail.credentials.username=mailer
mail.credentials.password=password1
mail.credentials.authMethod=SHA1
"}


   {:key "mailer-service"
    :version "1.0.2"
    :env "uat"
    :content-type "properties"
    :encoded true
    :value
    "
# Simple properties
mail.hostname=mailer@mail.com
mail.port=9000
mail.from=mailer@mail.com

# List properties
mail.defaultRecipients[0]=admin@mail.com
mail.defaultRecipients[1]=owner@mail.com

# Map Properties
mail.additionalHeaders.redelivery=true
mail.additionalHeaders.secure=true

# Object properties
mail.credentials.username=mailer
mail.credentials.password=jy1te2wi
mail.credentials.authMethod=SHA1
"}


   {:key "mailer-service"
    :version "1.0.0"
    :env "prod"
    :content-type "properties"
    :encoded true
    :value
    "
# Simple properties
mail.hostname=mailer@mail.com
mail.port=9000
mail.from=mailer@mail.com

# List properties
mail.defaultRecipients[0]=admin@mail.com
mail.defaultRecipients[1]=owner@mail.com

# Map Properties
mail.additionalHeaders.redelivery=false

# Object properties
mail.credentials.username=mailer
mail.credentials.password=mo4ni3vy
mail.credentials.authMethod=SHA1
"}


   {:key "mailer-service"
    :version "1.0.0"
    :env "prod"
    :content-type "properties"
    :encoded true
    :value
    "
# Simple properties
mail.hostname=mailer@mail.com
mail.port=9000
mail.from=mailer@mail.com

# List properties
mail.defaultRecipients[0]=admin@mail.com
mail.defaultRecipients[1]=owner@mail.com

# Map Properties
mail.additionalHeaders.redelivery=true
mail.additionalHeaders.secure=true

# Object properties
mail.credentials.username=mailer
mail.credentials.password=mo4ni3vy
mail.credentials.authMethod=SHA1
"}
   ])



(defn create-entries
  [entries]
  (doseq [entry entries]
    (save backend entry)))


(comment

  ;; DELETE ALL THE ITEMS
  (clear-table)


  (create-entries
   (concat
    user-service
    notification-service
    database
    mailer-service))

  )
