package com.example.demo;

import io.confluent.parallelconsumer.ParallelConsumerOptions;
import io.confluent.parallelconsumer.ParallelStreamProcessor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

public class KafkaParallelConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaParallelConsumer.class);
    private static final String TOPIC = "current-2025-events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String GROUP_ID = "parallel-consumer-group-" + System.currentTimeMillis();
    private static final int TARGET_RECORDS = 50_000; // Stop after 50K records
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private long startTime;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java KafkaParallelConsumer <mode>");
            System.out.println("mode: parallel or normal");
            return;
        }

        String mode = args[0].toLowerCase();
        KafkaParallelConsumer consumer = new KafkaParallelConsumer();
        
        if ("parallel".equals(mode)) {
            consumer.startParallel();
        } else if ("normal".equals(mode)) {
            consumer.startNormal();
        } else {
            System.out.println("Invalid mode. Use 'parallel' or 'normal'");
        }
    }

    private void startParallel() {
        log.info("Starting parallel consumer...");
        startTime = System.currentTimeMillis();
        
        // Configure Kafka consumer properties
        Properties props = getConsumerProperties();

        // Create Kafka consumer
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);

        // Configure Parallel Consumer options
        ParallelConsumerOptions<String, String> options = ParallelConsumerOptions.<String, String>builder()
                .ordering(ParallelConsumerOptions.ProcessingOrder.KEY)
                .maxConcurrency(32)
                .consumer(kafkaConsumer)
                .commitInterval(Duration.ofSeconds(5))
                .build();

        // Create Parallel Consumer
        ParallelStreamProcessor<String, String> parallelConsumer = ParallelStreamProcessor.createEosStreamProcessor(options);

        // Subscribe to topic
        parallelConsumer.subscribe(java.util.Collections.singletonList(TOPIC));
        log.info("Subscribed to topic: {}", TOPIC);

        // Create a latch to wait for completion
        CountDownLatch completionLatch = new CountDownLatch(1);

        // Process messages
        parallelConsumer.poll(context -> {
            try {
                ConsumerRecord<String, String> consumerRecord = context.getSingleRecord().getConsumerRecord();
                
                processMessage(consumerRecord);
                
                int count = messageCount.get();
                if (count >= TARGET_RECORDS) {
                    log.info("Reached target of {} records, signaling completion", TARGET_RECORDS);
                    completionLatch.countDown();
                }
            } catch (Exception e) {
                log.error("Error processing record", e);
            }
        });

        try {
            // Wait for completion
            completionLatch.await();
            log.info("Completion signal received, shutting down...");
            
            // Close the consumer
            parallelConsumer.close();
            kafkaConsumer.close();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("Finished processing {} records in {} ms ({} msgs/sec)", 
                    TARGET_RECORDS, 
                    duration,
                    String.format("%.2f", (TARGET_RECORDS * 1000.0) / duration));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted", e);
        }
    }

    private void startNormal() {
        log.info("Starting normal consumer...");
        startTime = System.currentTimeMillis();
        
        // Configure Kafka consumer properties
        Properties props = getConsumerProperties();

        // Create Kafka consumer
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(java.util.Collections.singletonList(TOPIC));

            while (messageCount.get() < TARGET_RECORDS) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    processMessage(record);
                    if (messageCount.get() >= TARGET_RECORDS) {
                        break;
                    }
                }
                consumer.commitSync();
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("Finished processing {} records in {} ms ({} msgs/sec)", 
                    TARGET_RECORDS, 
                    duration,
                    String.format("%.2f", (TARGET_RECORDS * 1000.0) / duration));
        }
    }

    private Properties getConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "2000");
        return props;
    }

    private void processMessage(ConsumerRecord<String, String> record) {
        // Extract message number from the value (format: "Message-{number}")
        String[] parts = record.value().split("-");
        int messageNumber = 0;
        if (parts.length > 1) {
            try {
                messageNumber = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // Ignore, leave messageNumber as 0
            }
        }
        // Calculate deterministic processing time between 1-5 milliseconds
        int processingTime = 1 + (messageNumber % 5);
        try {
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted", e);
        }
        int count = messageCount.incrementAndGet();
        if (count % 10_000 == 0) {  // Changed from 100_000 to 10_000 for more frequent updates
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - startTime;
            double rate = (count * 1000.0) / duration;
            log.info("Processed {} records in {} ms ({} msgs/sec)", 
                    count, 
                    duration, 
                    String.format("%.2f", rate));
        }
    }
} 