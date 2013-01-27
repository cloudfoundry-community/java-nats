[Spring NATS API Documentation](http://mheath.github.com/jnats/apidocs/0.3/client-spring/index.html)

## Configuring a NATS client in Spring

To use NATS with Spring, include the NATS client XML namespace in your Spring context configuration file as shown in
the following example:

 ```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        default-lazy-init="false"
        xmlns:nats="http://mheath.github.com/jnats/schema/spring/nats"
        xsi:schemaLocation="
        http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.1.xsd
        http://mheath.github.com/jnats/schema/spring/nats http://mheath.github.com/jnats/schema/spring/nats-0.5.xsd
        "
        >
...
</beans>
 ```

You can now configure a NATS client bean doing the following:

```xml
<nats:nats>
	<nats:url>nats://localhost:4222</nats:url>
</nats:nats>
```

This creates a Spring bean with the id "nats" of type `nats.client.Nats`.

## Annotation support

You can also configure annotation support for working with NATS. So instead of doing something like the following to
subscribe to a NATS subject:

```java
@Inject
private nats.client.Nats nats;

...

public void myMethod() {
	nats.subscribe("some.nats.subject", new MessageHandler() {
	@Override
		public void onMessage(Message message) {
			System.out.println("Received: " + message);
		}
	});
}
```

You can simply do:

```java

@Subscribe("some.nats.subject")
public void myMethod(Message message) {
        System.out.println("Received: " + message);
}
```

To configure annotation support, simply add the following to your Spring context XML:

```xml
<nats:annotation-config />
```

This, of course, assumes you have already configured a NATS client in Spring.
