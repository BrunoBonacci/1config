# FAQ

In this section I'm going to cover some of the most frequently asked questions:

### Q: Does the application get notified when the configuration changes?

`Answer`: **No, and there is a good reason for not doing it.** *It is
a perfect recipe for a full outage.* If the configuration system would
notify the running instances about the change and the instances would
react by reloading the new configuration they could all go down at the
same time.  Imagine you have 1000 instances of an application running,
now an operator changes the configuration which causes a malfunction
in the application. If all instances gets notified about at change and
reload the bad configuration change, instances would stop working
pretty much at the same time (even though you have 1000 redundant
copy).  This is just one way in which things can go wrong which such
"automated" reload, there are many others. Consider the self-similar
behaviour caused by all instances restarting, and connecting to the DB
or any external resource. It would cause the system to self-DDOS.

For the above reasons *1Config* will not notify running instances
about the change however if you still think it is a good idea for your
specific case it is fairly easy to implement by subscribing the
application to the [DynamoDB stream](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Streams.html)
for the *1Config* table.  When the application observes a change for
it own configuration, then it can issue another call to the
`configure` API to refresh the configuration.


### Q: Why would I need 1Config if I'm using Kubernetes Secrets?

`Answer`: There are many advantages using 1Config for your application
secrets compared to Kubernetes Secrets.

**Firstly, it is more secure!**, the secrets are store encrypted only
and read directly by the application (no intermediary files or
environment variables).  Kubernetes Secrets aren't stored encrypted by
defaults unless you configured the encryption correctly then you have
the risk that your master encryption key lies together with your
encrypted content (a feast for a hacker). For more info watch:
https://www.youtube.com/watch?v=7jSfJombUeY

The second advantage of 1Config is that it uses a hierarchical key
encryption which allow master key to be rotated easily and
automatically without manual intervention. With envelope encryption
(see: [Security Model](./security-model.md)) model, a different key is
used for every configuration entry; therefore if one key gets
compromised, only one entry can be read.

Finally, it uses DynamoDB as storage which is a Highly Available
datastore!  This is really important in comparison to *etcd*, *consul*
or *zookeeper* as these systems are designed to sacrifice high
availability in favour of consistency. So in case of a wide outage,
these systems will prefer not to answer instead of giving a
potentially outdated configuration.  Typically this is the wrong
choice in a large scale environment as in case of large outages you
will try to restart resources elsewhere and you need your
configuration to be available. Netflix built a system called
[Eureka](https://github.com/Netflix/eureka) to work as service
registry and metadata registry for service auto-discovery. They were
previously storing this information in Zookeeper, but after they
suffered a major outage which affected the Zookeeper ensemble, they
decided to build Eureka to provide High Availability.


### Q: How 1Config compares to AWS Secret Manager?

`Answer`: Both use the same encryption structure, the advantage of
1Config is that you can have multiple concurrent versions of the same
application running without conflicting configurations. Same it goes
for multiple environments in the same AWS account.


### Q: Why don't you use GraalVM for building native-images?

`Answer`: I'm working on it! In fact I've dedicated a full project on
building non-trivial Clojure projects with GraalVM. Some of the
libraries still have issues, and finding the correct GraalVM
configuration is not easy. For more information check:
https://github.com/BrunoBonacci/graalvm-clojure


### Q: Can I change the colors of the environments in the UI?

`Answer`: Yes, please check the **User preference** section of [User
profiles](./user-profiles.md)
