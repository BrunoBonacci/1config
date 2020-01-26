# File-based configuration for local development

`1config` implements a hierarchical search when it looks for valid
configurations. It searches in many different locations and a valid
configuration. For more information please see the [Configuration
Providers](./providers.md) for the details.

If you wish to use `1config` for local development (without a
connection to AWS DynamoDB) it is possible by creating entries
in your `1config` home directory (`~/.1config/`)

You can create a files under `~/.1config/` (in your home) and put the
configuration for one or more services in one or more environments
with the following format:
`~/.1config/<service-key>/<env>/<version>/<service-key>.<ext>`

For example, these are all valid entries:

  - `~/.1config/service1/dev/3.2.0/service1.edn`
  - `~/.1config/service1/dev/3.10.6/service1.txt`
  - `~/.1config/user-database/dev/1.0.0/user-database.properties`
  - `~/.1config/service1/staging/3.10.6/service1.json`

The intended use of the configuration in `~/.1config/` is to facilitate
development and allow the service to start with a local configuration
which doesn't reside in your code.

The easiest way to set the configuration is by using `1cfg` command line tool.

``` bash
$ 1cfg list -b fs
|------------+-----+---------+------------+------+-----------|
| Config key | Env | Version | Change num | Type | Timestamp |
|------------+-----+---------+------------+------+-----------|
|------------+-----+---------+------------+------+-----------|
(*) Timestamp is in local time.
```

Now create one entry:

``` bash
$ 1cfg -b fs -k service1 -e local -v 1.2.3 -t edn set '{:server {:port 8080}}'
```

Now we can see it with the `list` command:

``` bash
$ 1cfg list -b fs
|------------+-------+---------+---------------+------+----------------------|
| Config key |  Env  | Version |   Change num  | Type |       Timestamp      |
|------------+-------+---------+---------------+------+----------------------|
| service1   | local | 1.2.3   | 1580063063132 | edn  | 2020-01-26  18:24:23 |
|------------+-------+---------+---------------+------+----------------------|
(*) Timestamp is in local time.
```

Now let's inspect the content of the `1config` home:

``` bash
$ find ~/.1config
~/.1config
~/.1config/user-profiles.edn
~/.1config/service1
~/.1config/service1/local
~/.1config/service1/local/1.2.3
~/.1config/service1/local/1.2.3/service1.edn
```

And if we check the content, no surprise, it's what we set:

``` bash
$ cat ~/.1config/service1/local/1.2.3/service1.edn
{:server {:port 8080}}
```

**NOTE:** *It is convention to use `local` environment name for local
development* but it is not a requirement the name of the environment
can be anything.

**NOTE:** *Be aware that if a config entry exists in the local
filesystem (via `-b fs`) **it will supersede the dynamo backend**,
even if a better match exists in the dynamo backend.* This is to allow
local overrides of remote configurations.
