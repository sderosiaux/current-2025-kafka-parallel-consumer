package com.example.demo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class KafkaBenchmarkProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaBenchmarkProducer.class);
    private static final String TOPIC = "current-2025-events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int NUM_MESSAGES = 10_000_000; // 10 million messages

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            AtomicInteger counter = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < NUM_MESSAGES; i++) {
                String key = String.valueOf(i % 100); // Use 100 different keys for better parallelism
                String value = String.format("Message-%d", i);
                
                producer.send(new ProducerRecord<>(TOPIC, key, value), (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error sending message", exception);
                    } else {
                        int count = counter.incrementAndGet();
                        if (count % 1_000_000 == 0) { // Report every million messages
                            log.info("Produced {} million messages", count / 1_000_000);
                        }
                    }
                });
            }

            producer.flush();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("Finished producing {} messages in {} ms ({} msgs/sec)", 
                    NUM_MESSAGES, 
                    duration,
                    (NUM_MESSAGES * 1000L) / duration);
        }
    }
} 