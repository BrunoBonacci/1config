# Migration Procedure

Since v0.20.0, 1Config uses a different storage format.
v0.16.4 used KMS in combination with the [aws-encryption-sdk](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/introduction.html)
which provided support for generating data keys and store them
along with the encrypted data using a custom [binary format](https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html).

For quite a long time I've tried to compile the `1cfg` CLI tool using
GraalVM. However, I failed to generate a valid configuration for
GraalVM native-image builder. The executable JAR of v0.16.4 works fine
but it rather slow. So I decided to drop the `aws-encryption-sdk` and
use KMS api directly. As of version v0.20.0 this is now the way 1cfg works.
This allowed me to produce native binaries which are 1 order of magnitude
faster than the executable sdk.

Rest assured, dropping the `aws-encryption-sdk` didn't weaken the security
of model of 1cfg, it just removed one encryption envelope, and since
the library doesn't manage the encryption material directly, it is as safe
as before to use.

**If you are starting new installation of 1cfg, it is recommended to start using v0.20.0 only.**

In order to migrate your existing installation I produced a new
*transition* version (`v0.17.0`). The version v0.17.0 is a transition
version between the old format v0.16.4 and the new persistence format
v0.20.0.Use this version only if you have an existing installation
that you want to migrate to v0.20.0+. Use v0.16.4 if you happy with
your current solution.

This version has a migration task to migrate the database to a new
persistence format. In addition to the one-time migration step, when
you `SET` a new configuration entry, the entry will be written in two
different format, the old one and the new one thus allowing the
version v0.20.0 to read it as well.

```
v0.20.0     GET     SET     LIST      (new format only)
                     ^       |
                     |       v
v0.17.0     GET     SET     LIST
             |       |       |
             v       v       v
v0.16.4     GET     SET     LIST      (old format only)
```


The version v0.20.0 can only `GET` and `SET` entries with the new format,
while v0.17.0 will `GET` and `LIST` work as before, `SET` will write both
formats.

In the next section I will describe the steps required to migrate to the new
version of *1Config.

## Migration step

#### (1). Download v0.17.0
We will call this version `1cfgM` with `M` for "migration".
``` bash
mkdir -p ~/bin
wget https://github.com/BrunoBonacci/1config/releases/download/0.17.0/1cfg -O ~/bin/1cfgM
wget https://github.com/BrunoBonacci/1config/releases/download/0.17.0/1cfg-ui-beta -O ~/bin/1cfg-ui-betaM
chmod +x ~/bin/1cfgM ~/bin/1cfg-ui-betaM
export PATH=~/bin:$PATH
```

#### (2). Now we can run the *one-time* database migration.

Please ensure that you have access to all the KMS keys. The following
step is a **non-destructive operation** and it is **idempotent**. What
it does is it creates a new entry for every entry in your database
with the new format. **All existing entries remain untouched**.  This
ensures that all existing application will continue to work until the
time they are upgraded.

``` bash
# set your credentials
export AWS_ACCESS_KEY_ID=xxx
export AWS_SECRET_ACCESS_KEY=yyy
export AWS_REGION=eu-west-1

# run the migration
1cfgM MIGRATE-DATABASE
```

If you want to test this step on a copy of the Database, you can do so
by using the Point-In-Time Snapshot restore of DynamoDB which it will
create a new copy of the table.

Once you have the new table restore you can run:
``` bash
# run the migration on the table 1ConfigMigration
ONECONFIG_DYNAMO_TABLE=1ConfigMigration 1cfgM MIGRATE-DATABASE
```

You can use the similar command to test GET and SET.

#### (3) Once the migration is done, now it is time to upgrade the clients.

**Upgrade all the application to use the new library version** and redeploy the apps.
  - `[com.brunobonacci/oneconfig "0.20.0"]` for Leiningen projects
  - `{:deps { com.brunobonacci/oneconfig {:mvn/version "0.22.0"} }}` for `deps.edn` projects
  - For Java projects
```
<!-- https://mvnrepository.com/artifact/com.brunobonacci/oneconfig -->
<dependency>
    <groupId>com.brunobonacci</groupId>
    <artifactId>oneconfig</artifactId>
    <version>0.22.0</version>
    <classifier>aot</classifier>
</dependency>
```

The new version will look for config-entries with the new format only.
So if you have to change the configuration, please use the `1cfgM` as it will write
the new configuration entry in both formats.

#### (4) Download the new native-client

Follow the instruction in the [quick-start](./quick-start.md) guide.
