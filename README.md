# Java  HTTP clients benchmark

The purpose of this project is to show a comparative benchmark of various java http client libraries
by running those on a similar execution context scenario.  
Please note that this benchmark no attempts at being scientific.

## Requirements

- JDK >= 11. See  `gradle.porperties` for override property.

## How to run

```
./gradlew

# How tp override default values
./gradlew -Pbenchmark.concurrency=100
./gradlew -Pbenchmark.keep.alive.scenario=false -Pbenchmark.requests=2000 -Pbenchmark.client.socket.timeout.millis=120000 -Pbenchmark.client.connect.timeout.millis=30000

```

- Default gradle tasks are: `clean`, `benchmark`
- Customize benchmark by using `gradle.porperties` or `-p` on gradle run.
