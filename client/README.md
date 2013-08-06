[NATS API Documentation](http://cloudfoundry-community.github.com/java-nats/apidocs/0.5.1/client/index.html)

This NATS client is thread-safe.

## Basic Usage

```java
// Connecting to server
Nats nats = new NatsConnector().addHost("nats://localhost:4222").connect();

// Simple subscriber
nats.subscribe("foo", new MessageHandler() {
    @Override
    public void onMessage(Message message) {
        System.out.println("Received: " + message);
    }
});

// Simple publisher
nats.publish("foo", "Hello world!");


// Requests
nats.request("help", 1, TimeUnit.MINUTE, new MessageHandler() {
    @Override
    public void onMessage(Message message) {
        System.out.println("Got a response: " + message);
    }
});

// Replies
nats.subscribe("help", new MessageHandler() {
    @Override
    public void onMessage(Message message) {
        message.reply("I'll help!");
    }
});

// Close Nats
nats.close();
```

## Subscription Usage
```java

Subscription subscription = nats.subscribe("foo");

// Multiple message handlers
subscription.addMessageHandler(new MessageHandler() {
    public void onMessage(Message message) {
        System.out.println("Handler 1");
    }
});
subscription.addMessageHandler(new MessageHandler() {
    public void onMessage(Message message) {
        System.out.println("Handler 2");
    }
});

// Block until a message arrives (message handlers still get called)
NatsIterator iterator = subscription.iterator();
Message message = iterator.next();

// Or we can block for a limited amount of time
message = iterator.next(1, TimeUnit.MINUTE);

// Or we can just use a for loop
for (Message message : subscription) {
   System.out.println(message);
}

// Unsubscribing
subscription.close();

// Auto unsubscribe after receiving a set number of messages
subscription = nats.subscribe("foo", 2);

```

## Wildcard Subscriptions

```java
// "*" matches any token, at any level of the subject.
nats.subscribe("foo.*.baz");
nats.subscribe("foo.bar.*");
nats.subscribe("*.bar.*");

// ">" matches any length of the tail of a subject and can only be the last token
// For example, 'foo.>' will match 'foo.bar', 'foo.bar.baz', 'foo.foo.bar.bax.22'
nats.subscribe("foo.>");

// You can also match all messages
nats.subscribe(">");
```

## Queues Groups

```java
// All subscriptions with the same queue name will form a queue group
// Each message will be delivered to only one subscriber per queue group, queuing semantics
// You can have as many queue groups as you wish
// Normal subscribers will continue to work as expected.
nats.subscribe(subject, "job.workers").addMessageListener(...);
```

## Advanced Usage

```java
// Add multiple NATS servers when using NATS clustering
Nats nats = new NatsConnector().addHost("nats://host1").addHost("nats://host2").connect();

// Multiple connections are not really advanced in this NATS client but here's how to do it.
NatsConnector connector = new NatsConnector().addHost("nats://host1");
Nats nats1 = connector.connect();
Nats nats2 = connector.connect();
```

