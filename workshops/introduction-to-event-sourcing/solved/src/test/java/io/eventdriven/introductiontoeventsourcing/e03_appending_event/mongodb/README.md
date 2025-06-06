# Exercise 03 - Appending events to EventStoreDB

Using a defined structure of events from the [first exercise](../../e01_events_definition), fill a `appendEvents` function to store events in [EventStoreDB](https://developers.eventstore.com/clients/grpc/).

## Prerequisites
Run [docker-compose](../../../../../../../docker-compose.yml) script from the main workshop repository to start EventStoreDB instance.

```shell
docker compose up
```

or for Mac:

```shell
docker compose -f docker-compose.arm.yml up
```

After that you can use EventStoreDB UI to see how streams and events look like. It's available at: http://localhost:2113/.

## Solution

Use [Append events EventStoreDB API](https://developers.eventstore.com/clients/grpc/appending-events.html).
