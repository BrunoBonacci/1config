# Command line tool (`1cfg`)

`1Config` comes with a command line tool which allows you to
initialise and set values in the given backend.

You can install the tool via Homebrew or manually, please check the
[Quick Start](quick-start.md) page.

Download latest release from github and save it in your `~/bin` folder:

  * https://github.com/BrunoBonacci/1config/releases

**NOTE: It requires JDK/JRE 8+ to be installed and in the PATH.**

Then give it permissions to run:

``` shell
chmod +x ~/bin/1cfg
```

Here how to use it:

  * Initialize AWS ENV variables
  ``` bash
  export AWS_ACCESS_KEY_ID=xxx
  export AWS_SECRET_ACCESS_KEY=yyy
  export AWS_REGION=eu-west-1
  ```

  If you have the AWS CLI setup then you can use switch to the given profile with:
  ``` bash
  export AWS_PROFILE=xxx
  ```

  * Then run the tool
  ``` bash
  $ 1cfg --help

  ```

Here is how to use the command line:

``` text

   A command line tool to manage application secrets and configuration safely and effectively.

   Usage:

   1cfg <OPERATION> -e <ENVIRONMENT> -k <SERVICE> [-v <VERSION>] [-b <BACKEND>] [-t <TYPE>] <VALUE>

   OPERATION:
   ---------
      - GET        : retrieves the current configuration value for
                   : the given env/service/version combination
      - SET        : sets the value of the given env/service/version combination
      - LIST       : lists the available keys for the given backend
      - DIFF       : compares two config-entries and displays the differences
      - INIT       : initialises the given backend (like create the table if necessary)
      - LIST-KEYS  : lists the master encryption keys created by 1Config.
      - CREATE-KEY : creates an master encryption key.

   GENERAL OPTIONS:
   ---------------
   -h   --help                 : this help
        --stacktrace           : To show the full stacktrace of an error
   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical


   --------------------------------------------------------------------------------------------------
                                  --- backend initializaiton ---
   --------------------------------------------------------------------------------------------------

   ---| INIT |---------------------------------------------------------------------------------------
   (*) To initialise a given backend by creating the 1Config DynamoDB table (first time only)
   1cfg INIT -b dynamo



   --------------------------------------------------------------------------------------------------
                                     --- keys management ---
   --------------------------------------------------------------------------------------------------

   (*) List KMS encryption keys managed by 1Config
   1cfg LIST-KEYS

   (*) Create a master encryption key, the key name must be the same
       and the configuration key to be used automatically.
   1cfg CREATE-KEY -m service1
   -m   --master-key  KEY-NAME : The master encryption key to use for encrypting the entry.
                               : It must be a KMS key alias or an arn identifier for a key.


   --------------------------------------------------------------------------------------------------
                             --- configuration entries management  ---
   --------------------------------------------------------------------------------------------------

   ---| SET |----------------------------------------------------------------------------------------
   [#] sets the value of the given env/service/version combination

   1cfg SET [-b <BACKEND>] -e <ENVIRONMENT> -k <SERVICE> -v <VERSION>
            [-t <TYPE>] <VALUE> | -f /path/to/file.type

   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db-pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
                               : If not provided, the latest version will be returned.
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
   -f   --content-file FILE    : read the value to SET from the given file.
   -t   --content-type TYPE    : one of 'edn', 'txt' or 'json', 'properties' or 'props'
                               : default is 'edn'
   -m   --master-key  KEY-NAME : The master encryption key to use for encrypting the entry.
                               : It must be a KMS key alias or an arn identifier for a key.


   (*) To set the configuration value of a service called 'service1' use:
   1cfg SET -b dynamo -e test -k service1 -v 1.6.0 -t edn '{:port 8080}'

   (*) To set the configuration value of a service called 'service1' from a file
   1cfg SET -b dynamo -e test -k service1 -v 1.6.0 -t edn -f /tmp/config-file.edn

   (*) To set the config value for 'service1' but use a different key use:
   1cfg SET -b dynamo -e test -k service1 -v 1.6.0 -t edn '{:port 8080}' -m team1-key


   ---| GET |----------------------------------------------------------------------------------------
   [#] retrieves the current configuration value for the given env/service/version combination

   1cfg GET [-b <BACKEND>] -e <ENVIRONMENT> -k <SERVICE> [-v <VERSION>] [-c <CHANGE-NUM]

   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db-pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
                               : If not provided, the latest version will be returned.
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
        --with-meta            : whether to include meta data for GET operation
   -P   --pretty-print         : whether to pretty print the configuration values


   (*) To read last configuration value for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k service1

   (*) To read last change for a specific version of 'service1' use:
   1cfg GET -b dynamo -e test -k service1 -v 1.6.0

   (*) To read a specific changeset for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k service1 -v 1.6.0 -c 3563412132


   ---| DIFF |---------------------------------------------------------------------------------------
   [#] compares two config-entries and displays the differences

   1cfg DIFF <config-entry1> <config-entry2>
   1cfg <config-entry1> DIFF <config-entry2>

   NOTE: when backend, env, version, change-num are omitted it uses the values from the first entry

   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db-pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
                               : If not provided, the latest version will be returned.
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
   -D   --diff-mode    MODE    : either 'line' or 'char' (default is 'line')
        --DC                   : same as '--diff-mode char'


   (*) specify two entries to compare
   1cfg DIFF -b dynamo -e test -k service1 -v 1.6.0 -c 3563412132 \
             -b dynamo -e test -k service1 -v 1.6.0 -c 3563413434

   (*) you can omit parameters which are the same, so you can rewrite above as:
   1cfg DIFF -b dynamo -e test -k service1 -v 1.6.0 -c 3563412132 -c 3563413434

   (*) compare entries between two environment for same service
   1cfg -e test -k service1 -v 1.6.0 DIFF -e prod

   (*) compare entries between two environment for same service
   1cfg -k service1 -v 1.6.0 -e test DIFF -e prod

   (*) compare entries for the same service and same environment but different versions
   1cfg -e test -k service1 -v 1.6.0 DIFF -v 1.7.0

   (*) compare change-sets from the same service
   1cfg -e test -k service1 -v 1.6.0 -c 3563412132 DIFF -c 35634567567


   ---| LIST |---------------------------------------------------------------------------------------
   [#] lists the available keys for the given backend

   1cfg LIST [-b <BACKEND>] [-e <ENVIRONMENT>] [-k <SERVICE>] [-v <VERSION>] [-o ORDER-BY]
   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db-pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
   -C                          : same as '--output-format=cli'
   -X   --extended             : whether to display an extended table (more columns)
   -o   --order-by     ORDER   : The listing order, must be a comma-separated list
                               : of one or more of: 'key', 'env', 'version', 'change-num'
                               : default order: 'key,env,version,change-num'

   (*) To list all the entries
   1cfg LIST

   (*) To list all the entries in extended mode
   1cfg LIST -X

   (*) List entries with filters
   1cfg LIST -e prod
   1cfg LIST -k ser
   1cfg LIST -e test -v 1.

   (*) To list all the entries with specific ordering
   1cfg LIST -o env,key

   (*) To list all the entries in command-line mode
   1cfg LIST -C
   --------------------------------------------------------------------------------------------------

   NOTE: - set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_PROFILE to provide authentication
           and access to the target AWS account.
         - set AWS_REGION to set the AWS region to use.

```
