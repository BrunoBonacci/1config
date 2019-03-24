# 1config
[![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/oneconfig.svg)](https://clojars.org/com.brunobonacci/oneconfig) ![CircleCi](https://img.shields.io/circleci/project/BrunoBonacci/oneconfig.svg) ![last-commit](https://img.shields.io/github/last-commit/BrunoBonacci/oneconfig.svg) [![Dependencies Status](https://jarkeeper.com/BrunoBonacci/safely/status.svg)](https://jarkeeper.com/BrunoBonacci/oneconfig)

A library to manage environments configuration at application level.
Here some of the key-points and advantages:

  * Easy way to retrieve and manage configuration for your AWS deployed services
  * Compatible with AWS Lambdas as well
  * AWS KMS encryption based security (same as S3-SSE, EBS and RDS)
  * Support for multiple environments in the same AWS account
  * Support for multiple concurrent versions
  * Zero config approach (or at most 1 config `;-)`)
  * Anti-tampering checks for configuration entries
  * Supports Clojure, Java, Groovy, and other JVM languages (more to come)
  * Command line tool for managing changes to the configuration
  * Support for local development (outside AWS)
  * Highly-configurable and secure authorization.

## Usage


## Usage with Clojure

In order to use the library add the dependency to your `project.clj`

``` clojure
;; Leiningen project
[com.brunobonacci/oneconfig "0.1.0-SNAPSHOT"]

;; deps.edn format
{:deps { com.brunobonacci/oneconfig "0.1.0-SNAPSHOT" }}
```

Latest version: [![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/oneconfig.svg)](https://clojars.org/com.brunobonacci/oneconfig)


Then require the namespace:

``` clojure
(ns foo.bar
  (:require [com.brunobonacci.oneconfig :refer [configure]]))
```

Finally get the configuration for your service.

``` clojure
(configure {:key "service-name" :version "1.2.3" :env "prod"})
;;=> {...}
```


## Usage with Java

In order to use the client library add the
[Clojars.org](https://clojars.org/) Maven repository in your `pom.xml`
and add the dependency:

Add the repository:
``` xml
<repository>
  <id>clojars.org</id>
  <url>https://clojars.org/repo</url>
</repository>
```

Then add the dependency

``` xml
<!-- https://mvnrepository.com/artifact/com.brunobonacci/oneconfig -->
<dependency>
    <groupId>com.brunobonacci</groupId>
    <artifactId>oneconfig</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```
Latest version: [![Clojars Project](https://img.shields.io/clojars/v/com.brunobonacci/oneconfig.svg)](https://clojars.org/com.brunobonacci/oneconfig)


Then import the client, and request the configuration entry wanted:

``` java
// add the import
import com.brunobonacci.oneconfig.client.OneConfigClient;
import com.brunobonacci.oneconfig.client.OneConfigClient.ConfigEntry;
// ....

// then in your code retrieve the config:
ConfigEntry config = OneConfigClient.configure("service1", "dev", "1.8.0");

// check if configuration is found
if ( config == null )
    throw new RuntimeException("Unable to load configuration");

// retrieve the value:
config.getValueAsString();        // for txt entries
config.getValueAsProperties();    // for properties entries
config.getValueAsJsonMap();       // Map<String, Object> for json entries
config.getValueAsEdnMap();        // Map<Keyword, Object> for edn entries
```


### Configuration resolution

`configure` will try a number of different location to find a configuration provider.
It will attempt to read a file or dynamo table in the following order.

  * Java System property `1config.file`, if set and the file exists
    it will be used as *sole* configuration
  * Environment variable `$ONECONFIG_FILE`, if set and the file exists
    it will be used as *sole* configuration
  * Java Resource bundle `1config.edn` (or `.json`, `.txt`,
    `.properties`), if present it will be used as *sole* configuration
  * `~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>`
    (home dir) - if present it will be used as configuration.
    Entries in the `~/.1config/` will have precendence over the DynamoDB table.
  * DynamoDB table called `1Config` in the "current" region.

The name of the DynamoDB table can be customized with
`$ONECONFIG_DYNAMO_TABLE` environment variable. It will use the
machine role to access the database. The AWS region can be controlled
via the environment variable `$AWS_DEFAULT_REGION`. For the AWS
credentials we use the [Default Credential Provider
Chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#id6).
Therefore the credentials can be provided in one of the following
ways:

  - **Environment Variables** – `AWS_ACCESS_KEY_ID` and
    `AWS_SECRET_ACCESS_KEY`. The AWS SDK for Java uses the
    `EnvironmentVariableCredentialsProvider` class to load these
    credentials.

  - **Java System Properties** – `aws.accessKeyId` and
    `aws.secretKey`. The AWS SDK for Java uses the
    `SystemPropertiesCredentialsProvider` to load these credentials.

  - **The default credential profiles file** – typically located at
    `~/.aws/credentials` (this location may vary per platform), this
    credentials file is shared by many of the AWS SDKs and by the AWS
    CLI. The AWS SDK for Java uses the ProfileCredentialsProvider to
    load these credentials.

    You can create a credentials file by using the aws configure
    command provided by the AWS CLI, or you can create it by
    hand-editing the file with a text editor. For information about
    the credentials file format, see AWS Credentials File Format.

  - Instance profile credentials – these credentials can be used on
    EC2 instances, and are delivered through the Amazon EC2 metadata
    service. The AWS SDK for Java uses the
    `InstanceProfileCredentialsProvider` to load these credentials.


### Configuration resolution.

A configuration entry is uniquely identified by **key, environment and version**.
While resolving the specific configuration the system if going  to look for a
exact version match or a version which is smaller than the given one.

For this reason you don't have to publish a new configuration for every
version change. For example: let's assume you have the following data.

|------------|-------|----------|----------------------------------------------------------------|
| Config key | Env   | Version  | value                                                          |
|------------|-------|----------|----------------------------------------------------------------|
| `service1` | `dev` | `2.1.0`  | `{:host "localhost", :port 1234}`                              |
| `service1` | `dev` | `3.7.0`  | `{:host "my.db.local" :port 1234 :user "test2" :pass "test2"}` |
| `service1` | `dev` | `3.10.0` | `{:host "my.db.local" :port 1234 :user "foo" :pass "bar"}`     |
|            |       |          |                                                                |


If you ask of a precisely matching configuration you get that specific
config entry or nil if not found:

``` clojure
;; key not found
(configure {:key "system-not-present" :env "dev" :version "3.1.0"})
;;=> nil


;; exact match
(configure {:key "system1" :env "dev" :version "2.1.0"})
;;=>
;; {:content-type "edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "2.1.0",
;;  :value {:host "localhost", :port 1234},
;;  :change-num 0}

```

If an exact match isn't found the system retrieve the previous
configuration is available

``` clojure
;; exact match not found, but previous version found
;; even across major versions
(configure {:key "system1" :env "dev" :version "3.6.2"})
;;=>
;; {:content-type "edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "2.1.0",
;;  :value {:host "localhost", :port 1234},
;;  :change-num 0}


;; if there aren't previous versions it returns nil
(configure {:key "system1" :env "dev" :version "1.1.0"})
;;=> nil

;; exact match not found, but previous version found
;; in this case the most recent (previous) version is selected.

(configure {:key "system1" :env "dev" :version "3.8.0"})
;;=>
;; {:content-type "edn",
;;  :env "dev",
;;  :key "system1",
;;  :version "3.7.0",
;;  :value {:host "my.db.local", :port 1234, :user "test2", :pass "test2"},
;;  :change-num 0}

```

As mentioned earlier, versions are sorted by numerical elements and not
by alphanumeric values.


### File based configuration.

This is mostly intended for local development, you can create a files
under `~/.1config/` (in your home) and put the configuration for one
or more services in one or more environments with the following
format: `~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>`

For example, these are all valid entries:

  - `~/.1config/service1/dev/3.2.0/service1.edn`
  - `~/.1config/service1/dev/3.10.6/service1.txt`
  - `~/.1config/user-database/dev/1.0.0/user-database.properties`
  - `~/.1config/service1/staging/3.10.6/service1.json`

The intended use of the configuration in `~/.1config/` is to facilitate
development and allow the service to start with a local configuration
which doesn't reside in your code.


### Limitations

There are a number of limitations to consider:

  * 1Config doesn't support `SNAPSHOT` versions, and I don't
    believe there is any valid use case for supporting them, so you
    must strip the `-SNAPSHOT` prior the call to `configure`.
  * 1Config only supports three-legged numerical versions such
    as `"1.12.2"` no other version qualifiers such as `alpha`, `beta`
    etc.


### Command line tool

1config comes with a command line tool which allows you to
initialise and set values in the given backend.

Download latest release from github and save it in your `~/bin` folder:

  * https://github.com/BrunoBonacci/1config/releases

Then give it permissions to run:

``` shell
chmod -x ~/bin/1cfg
```

Here how to use it:

``` text

  A command line tool for managing configurations in different environments.

Usage:

   1cfg <OPERATION> -e <ENVIRONMENT> -k <SERVICE> -v <VERSION> [-b <BACKEND>] [-t <TYPE>] <VALUE>

   WHERE:
   ---------

   OPERATION:
      - GET        : retrieve the current configuration value for
                   : the given env/service/version combination
      - SET        : sets the value of the given env/service/version combination
      - LIST       : lists the available keys for the given backend
      - INIT       : initialises the given backend (like create the table if necessary)
      - LIST-KEYS  : lists the master encryption keys created by 1Config.
      - CREATE-KEY : creates an master encryption key.

   OPTIONS:
   ---------
   -h   --help                 : this help
        --stacktrace           : To show the full stacktrace of an error
   -b   --backend   BACKEND    : only 'dynamo' is currently supported, and it is the default one.
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db.pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
        --with-meta            : whether to include meta data for GET operation
        --output-format FORMAT : either 'table' or 'cli' default is 'table' (only for list)
   -C                          : same as '--output-format=cli'
   -o   --order-by     ORDER   : The listing order, must be a comma-separated list
                               : of one or more of: 'key', 'env', 'version', 'change-num'
                               : default order: 'key,env,version,change-num'
   -t   --content-type TYPE    : one of 'edn', 'txt' or 'json', 'properties' or 'props'
                               : default is 'edn'
   -m   --master-key  KEY-NAME : The master encryption key to use for encrypting the entry.
                               : It must be a KMS key alias or an arn identifier for a key.

Example:

   --- keys management ---

   (*) List KMS encryption keys managed by 1Config
   1cfg LIST-KEYS

   (*) Create a master encryption key, the key name must be the same
       and the configuration key to be used automatically.
   1cfg CREATE-KEY -m 'service1'

   --- configuration entries management  ---

   (*) To initialise a given backend
   1cfg INIT -b dynamo

   (*) To set the configuration value of a service called 'service1' use:
   1cfg SET -b dynamo -e test -k 'service1' -v '1.6.0' -t edn '{:port 8080}'

   (*) To read last configuration value for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0'

   (*) To read a specific changeset for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0' -c '3563412132'

   (*) To list configuration with optional filters and ordering
   1cfg LIST -b dynamo -e prod -k ser -v 1. -o env,key


NOTE: set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_PROFILE to
      provide authentication access to the target AWS account.
      set AWS_DEFAULT_REGION to set the AWS region to use.

```

### AWS permissions

If you are using role based permissions then ensure that your role
has the following permissions included:

``` json
{
  "Statement": [
    {
        "Action": [
            "dynamodb:GetItem",
            "dynamodb:BatchGetItem",
            "dynamodb:Query"
        ],
        "Effect": "Allow",
        "Resource": "arn:aws:dynamodb:eu-west-1:*:table/1Config"
    }
  ]
}
```

For the command line tool you will need add the permission to create a
table as well.

## License

Copyright © 2019 Bruno Bonacci - Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
