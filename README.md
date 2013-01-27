# Java NATS Client

A Java client for the excellent NATS lightweight publish-subscribe and distributed queueing messaging system used by
[Cloud Foundry](http://cloudfoundry.com).

More information about NATS can be found at the [NATS Git Hub project](https://github.com/derekcollison/nats).

This project provides a [simple Java client for NATS](jnats/tree/master/client) as well as [Spring integration](jnats/tree/master/client-spring) for using the client.

To use the basic client in your project, add the following to your Maven pom.xml:
```xml
<dependency>
    <groupId>com.github.mheath</groupId>
    <artifactId>nats-client</artifactId>
    <version>0.4</version>
</dependency>
```

To use the optional Spring integration, add the following to your Maven pom.xml:

```xml
<dependency>
    <groupId>com.github.mheath</groupId>
    <artifactId>nats-client-spring</artifactId>
    <version>0.4</version>
</dependency>
```

## License

(The Apache Software License 2.0) - http://www.apache.org/licenses/

Copyright (c) 2012, 2013 Mike Heath

