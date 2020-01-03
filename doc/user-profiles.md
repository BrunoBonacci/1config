# User profiles

User profiles allow to set common information for a single
user/operator.  By creating a file `~/.1config/user-profiles.edn` you
can set some preferences. Here the general structure of the profile:


``` clojure
{:preferences
   ;; some defaults which can be changed
   {
    ;; The UI display labels for the various environment
    ;; these are the default colors which can be customized by the user
    :colors
    {:env-labels
     {"dev"     "green"
      "uat"     "yellow"
      "prod"    "red"
      :default  "grey"}}
    }

   ;; restriction of what can be set in a given account
   ;; good way to set hygiene rules and naming conventions.
   ;; the general form is
   ;;
   ;; [gurad condition] :-> [restriction] :message "user friendly error message"
   :restrictions []
   ;; here some examples
   ;; [:account :matches? ".*"] :-> [:key :matches-exactly? "[a-z][a-z0-9-]+"]
   ;; :message "Invalid service name, shoulbe lowercase letters, numbers and hypens (-)"
   ;;
   ;; [:account :is? "1234567890"] :-> [:env :in? ["prd" "dr-prd"]]
   ;; :message "Invalid environment for this account, only prd, and dr-prd are allowed"
   }
```


## User preferences

You can customize things like the color of the label for each environment
by creating a file `~/.1config/user-profiles.edn` which contains a map
with the following structure:

``` clojure
{:preferences
   ;; some defaults which can be changed
   {
    ;; The UI display labels for the various environment
    ;; these are the default colors which can be customized by the user
    ;; The environment names are case sensitive and must be an exact match.
    :colors
    {:env-labels
     {"dev"     "green"
      "uat"     "yellow"
      "prod"    "red"
      :default  "grey"}}
    }
   }
```

## User restrictions

In large teams you might want to add some constraints on naming
conventions or the name of the environment you are allowed to set.
Sometimes you might want to simply avoid common mistakes that
operators do, such as setting the wrong name or setting the wrong
content-type.

For all these cases you can constraint the use of `1cfg` with the
`:restrictions` key in the `user-profiles.edn`.

The restrictions allow to add constraints on the `1cfg SET` function.
For example you might want to introduce naming conventions, or for
example you might want to limit which environment is used in a
specific AWS account. Finally you can also avoid the common mistake of
setting a config entry with the wrong type.

All these can be constraint with the user of `:restrictions` in the
`user-profiles.edn`

The general structure of the restrictions is:

``` clojure
{:restrictions
 [
 ;; guard :-> restriction :message "Display message"
 ]}

```

Restrictions is a list of conditions. All will be tested.  The first
is the `guard` condition, which means that the `restriction` will
tested only if the `guard` condition is matched.

For example:

``` clojure
  ;; guard                  ;->  restriction                  :messsage "error to display"
  [:account :matches? ".*"] :->  [:key :matches-exactly? "[a-z][a-z0-9-]+"]
  :message "Invalid service name, shoulbe lowercase letters, numbers and hypens (-)"
```

This restriction imposes that for all accounts the `:key` should only
include lower-case letters and number with hyphens starting with a
letter.  The guard condition `[:account :matches? ".*"]` will match
all the AWS accounts including the `"local"` (when using the backend
`:fs`) and ensure that the `[:key :matches-exactly?
"[a-z][a-z0-9-]+"]`.  If this restriction test fails during a `1cfg
SET` operation then the given message will be displayed:

``` bash
$ 1cfg -b fs -e whatever -k 1bar -v 1.0.0 -t txt set 'hi'
ERROR: RESTRICTION: Invalid service name, shoulbe lowercase letters, numbers and hypens (-)
CAUSE: RESTRICTION: Invalid service name, shoulbe lowercase letters, numbers and hypens (-)
```

**NOTE**: The `RESTRICTION:` prefix is added to clarify that this
error comes from a failed restriction.

For example if you which to restrict the name of the environments
to be used for a particular account add:

``` clojure
  ;; AWS account number
  [:account :matches? "11111111111111"] :-> [:env :in? ["dev" "int" "uat" "prd"]]
  :message "Invalid environment, only dev, int, uat and prd are allowed"
```

If you wish to restrict the configuration type for a specific key you can use:

``` clojure
  ;; Ensure that `user-service` is always set with the corrent content-type
  [:key :is? "user-service"] :-> [:content-type :is? ""]
  :message "Invalid content type for user-service."
```

The available fields during the SET operations are:
`:env`,`:key`,`:version`,`:content-type`,`:value` and `:master-key`
when provided.
The available operators for the conditions are:

| Comparator        | Complement (not)      | Case-insensitive  | Insensitive Complement |
| -------------     | -----------------     | ----------------  | ---------------------- |
| :is?              | :is-not?              | :IS?              | :IS-NOT?               |
| :starts-with?     | :not-starts-with?     | :STARTS-WITH?     | :NOT-STARTS-WITH?      |
| :ends-with?       | :not-ends-with?       | :ENDS-WITH?       | :NOT-ENDS-WITH?        |
| :contains?        | :not-contains?        | :CONTAINS?        | :NOT-CONTAINS?         |
| :in?              | :not-in?              | :IN?              | :NOT-IN?               |
| :matches?         | :not-matches?         | :MATCHES?         | :NOT-MATCHES?          |
| :matches-exactly? | :not-matches-exactly? | :MATCHES-EXACTLY? | :NOT-MATCHES-EXACTLY?  |
|                   |                       |                   |                        |

Conditions can be also logically grouped with `:and` and `:or`, for
more info see: https://github.com/BrunoBonacci/where

To retrieve your aws account number using the AWS cli run:

``` bash
$ export AWS_PROFILE=chage_profile_if_needed
$ aws sts get-caller-identity --output json
{
    "UserId": "AIDAJHGJHGKJGJHGJH",
    "Account": "111111111111",
    "Arn": "arn:aws:iam::111111111111:user/operator"
}
```

Please note that the purpose of `:restrictions` in `user-profiles.edn`
is to avoid user mistakes not to limit capabilities. For this reason
the user can bypass a given restriction by commenting it in it's local
file.
