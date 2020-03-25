# Usage with Java projects

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
    <version>0.16.1</version>
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

If you are using Spring, there is a integration library which works
out of the box. Please check out
[Spring-1config](https://github.com/brandonstubbs/spring-1config)
