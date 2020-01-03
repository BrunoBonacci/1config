# Command line tool (`1cfg`)

`1Config` comes with a command line tool which allows you to
initialise and set values in the given backend.

Download latest release from github and save it in your `~/bin` folder:

  * https://github.com/BrunoBonacci/1config/releases

**NOTE: It requires JDK/JRE 8+ to be installed and in the PATH.**

Then give it permissions to run:

``` shell
chmod -x ~/bin/1cfg
```

Here how to use it:

``` text

  A command line tool to manage application secrets and configuration safely and effectively.

Usage:

   1cfg <OPERATION> -e <ENVIRONMENT> -k <SERVICE> [-v <VERSION>] [-b <BACKEND>] [-t <TYPE>] <VALUE>

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
   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db-pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
                               : If not provided, the latest version will be returned.
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
   -f   --content-file FILE    : read the value to SET from the given file.
        --with-meta            : whether to include meta data for GET operation
        --output-format FORMAT : either 'table' or 'cli' default is 'table' (only for list)
   -C                          : same as '--output-format=cli'
   -X   --extented             : whether to display an extended table (more columns)
   -P   --pretty-print         : whether to pretty print the configuration values
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

   (*) To initialise a given backend (first time only)
   1cfg INIT -b dynamo

   (*) To set the configuration value of a service called 'service1' use:
   1cfg SET -b dynamo -e test -k 'service1' -v '1.6.0' -t edn '{:port 8080}'

   (*) To read last configuration value for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1'

   (*) To read last change for a specific version of 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0'

   (*) To read a specific changeset for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0' -c '3563412132'

   (*) To list configuration with optional filters and ordering
   1cfg LIST -b dynamo -e prod -k ser -v 1. -o env,key


NOTE: set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_PROFILE to
      provide authentication access to the target AWS account.
      set AWS_DEFAULT_REGION to set the AWS region to use.

```
