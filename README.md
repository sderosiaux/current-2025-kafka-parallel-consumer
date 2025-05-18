# Kafka Parallel Consumer Demo

This project demonstrates the performance difference between a normal Kafka consumer and a parallel consumer implementation. It processes messages with simulated processing times between 1-5ms to showcase the benefits of parallel processing.

## Prerequisites

- Java 17 or higher
- Maven
- Kafka running locally on port 9092
- Topic `current-2025-events` created in Kafka

## Building the Project

```bash
mvn clean package
```

## Running the Tests

The project provides two modes of operation:

1. Parallel Consumer:
```bash
mvn compile exec:java -Dexec.mainClass="com.example.demo.KafkaParallelConsumer" -Dexec.args="parallel"
```

2. Normal Consumer:
```bash
mvn compile exec:java -Dexec.mainClass="com.example.demo.KafkaParallelConsumer" -Dexec.args="normal"
```

## Test Results

### Parallel Consumer
- Total time: 10.117 seconds
- Throughput: 4,942.18 messages/second
- Processing rate progression:
  - 1,799.21 msgs/sec at 10K records
  - 3,032.60 msgs/sec at 20K records
  - 3,929.79 msgs/sec at 30K records
  - 4,612.55 msgs/sec at 40K records
  - 5,120.85 msgs/sec at 50K records

### Normal Consumer
- Total time: 197.623 seconds (about 3.3 minutes)
- Throughput: 253.01 messages/second
- Processing rate progression:
  - 236.18 msgs/sec at 10K records
  - 245.58 msgs/sec at 20K records
  - 249.06 msgs/sec at 30K records
  - 252.28 msgs/sec at 40K records
  - 253.02 msgs/sec at 50K records

## Key Findings

1. The parallel consumer was approximately 19.5x faster than the normal consumer (4,942 vs 253 msgs/sec)
2. The parallel consumer showed increasing throughput over time due to:
   - Better thread pool utilization
   - Reduced overhead from thread creation
   - Better CPU cache utilization
3. The normal consumer maintained a relatively stable throughput, limited by its single-threaded nature

## Implementation Details

- Uses Confluent's Parallel Consumer library
- Processes 50,000 records in each test
- Simulates processing time between 1-5ms per message
- Uses deterministic processing time based on message number
- Maintains ordering by key in parallel mode
- Uses 32 concurrent threads in parallel mode

## Dependencies

- Apache Kafka Client
- Confluent Parallel Consumer
- SLF4J for logging

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