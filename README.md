# poise-sales-service
Microservice for Sales Domain
It owns the visit and purchase coordination workflow for the Sales Domain. It is responsible for:
- Managing the `Visit` lifecycle
- Orchestrating the booking saga with Inventory
- Handling dealer confirmation deadlines
- Emitting sales-domain events
- Maintaining a low-latency read model to serve BFF

This service is not a CRUD API. It is a workflow orchestrator built around CQRS, event-driven integration, and compensating transactions.

Technology
Quarkus  - LTS

Extensions (dependencies) - beyond a simple dependency - create boilerplate - code
<pom>
they will be placed here
</pom>

It can work along with Spring

Arc - CDI dependency injection
Comes with Dockerfiles (use JVM)

Quarkus is released twice a month.

Vertx - most powerful runtime for Java

mvn clean package -Pnative

his project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/sales-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides


## Provided Code

### gRPC

Create your first gRPC service

[Related guide section...](https://quarkus.io/guides/grpc-getting-started)

### Kafka

Message, event, streaming

Cluster
Composed by N Brokers

Brokers
Capable of distribute, replicate events

Producers
Can set partition
Partition key (FIFO), opposed to Streaming
ACK > throughput <
Batch size (message grouping) - otimization, less reliable
Linger time (work along with batch)

consumers
Do not share partition
Consumer Group (different functions for the same event)
Do not exist horizontal scalabitity (due to partition limitation)

topic
Category of events (feed)
Can have many consumers, by subscribing to this event

partition
Paralesmin for topis (as load balancer)
Sharding via partition key
Unbalance partitions

replication factor
When created a topic, make sure the topic can be replicated in other brokers
replicatio factor <= broker

### Message

Protocols
# MQTT
over TCP
Bandwith and network limit
Brokers and topics
Default subscription (1 * N)
Shared subscription (balanced)

# AMQP
more functionalities then MQTT
Used for distributed systems (RabbitMQ)

Exchange (routing/binding)
Many queues sharing the same exchange

Direct Exchanges
Standard - using binding keys to route (point-to-point)

Topic Exchanges
More flexible than, based on patterns defined, using wild cards, i.e. faturamento.#

Fanout Exchanges
Replicate from exchange to many queues
Same message to many queues

# Saga
Motivation
Consistency among services, multiple domains
Long-lived transactions (hours, days to "commit" transaction)

# Features
Each step has an action and compensation
More powerful ACID
Supports Eventual Consistency

## Choreography
Data pipeline
There is no central coordinator
Each microservice knows next step
Less overhead

Compensation
Service that fails activate rollback
Central topic can be used as Panic button

## Orchestration
Central orchestrator (like a Control Plane)
Workflow pattern
State Machine Pattern
Command / Reply (Topics)

### State Machine in Saga
Works well with Event Sourcing

Actual State -> Event Received -> Next state

- Compensation
Each action has a compensation step
Workflows to undo operations
Semantically inverted steps (book / unbook)
Adds consistency
Transactional logs for Saga (auditing/debbuging)
Using Dual Write (database + event)
Outbox and CDC helps on consistency