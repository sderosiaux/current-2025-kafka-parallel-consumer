# Maven Demo Project

This is a simple Java project created with Maven that includes a Kafka parallel consumer implementation.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Apache Kafka running locally on port 9092
- A Kafka topic named "test-topic" created in your local Kafka cluster

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Running the Application

To run the original demo application:

```bash
mvn exec:java -Dexec.mainClass="com.example.demo.App"
```

To run the Kafka parallel consumer:

```bash
mvn exec:java -Dexec.mainClass="com.example.demo.KafkaParallelConsumer"
```

## Running Tests

To run the tests, use:

```bash
mvn test
```

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/
│           └── example/
│               └── demo/
│                   ├── App.java
│                   └── KafkaParallelConsumer.java
└── test/
    └── java/
        └── com/
            └── example/
                └── demo/
                    └── AppTest.java
```

## Kafka Consumer Features

The Kafka parallel consumer implementation includes:
- Parallel message processing with key-based ordering
- Automatic offset management
- Graceful shutdown handling
- Message counting and logging
- Configurable concurrency (default: 16 threads)

## Configuration

The Kafka consumer is configured to:
- Connect to localhost:9092
- Use the consumer group "parallel-consumer-group"
- Process messages from the "test-topic" topic
- Start consuming from the earliest available offset 