# Changelog

## Release: v0.10.3 (2019-09-18)

  * Fixed various issue with Windows usage
  * Improved region detection
  * Improved error messages for when HOME directory is not set

## Release: v0.10.2 (2019-07-09)

  * Fixed issue with regions when running on EC2 when AWS_DEFAULT_REGION is not set

## Release: v0.10.1 (2019-06-09)

  * Fixed missing content-type when providing a configuration via `$ONECONFIG_FILE`

## Release: v0.10.0 (2019-06-09)

  * Added ability to restrict keys, envs etc via `user-profiles.edn`
  * Turned keys into string for Java client
  * Added getIn() capability into Java client

## Release: v0.9.2 (2019-04-10)

  * Retrieve AWS user using SecurityTokens APIs instead of IAM GetUser

## Release: v0.9.1 (2019-04-09)

  * Improved handling of SET on un-initialized fs backend.

## Release: v0.9.0 (2019-04-08)

  * Added ability to `save`/`SET` configuration entries into
    filesystem backend via the command line tool.

## Release: v0.8.0 (2019-04-02)

  * Added the `content-type` in the entry listing
  * Added backend selection
  * Added environment variable to select default backend
  * Added ability to provide the value directly from a file
  * Added `deepMerge` in java client

## Release: v0.7.0 (2019-03-28)

  * Added user into the encryption context (anti tampering)
  * Store values in verbatim to preserve comments
  * Added optional pretty-printing for values

## Release: v0.6.1 (2019-03-25)

  * Fixed issue when ~/.1config doesn't exists

## Release: v0.6.0 (2019-03-25)

  * Storing user details on save

## Release: v0.5.0 (2019-03-24)

  * Simple, multi-environment, multi-version, configuration control
  * AWS Cloud native
  * AWS KMS double encryption
  * Anti-tampering control
  * Support for `edn`,  `json`, `txt` and `properties` files
  * Scalable and fast DynamoDB backend storage
  * Command line tool to manage configurations
  * Support for Clojure, Java and other JVM languages
