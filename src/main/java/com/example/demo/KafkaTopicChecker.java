package com.example.demo;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

public class KafkaTopicChecker {
    private static final Logger log = LoggerFactory.getLogger(KafkaTopicChecker.class);
    private static final String TOPIC = "current-2025-events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "topic-checker");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(java.util.Collections.singletonList(TOPIC));
            log.info("Subscribed to topic: {}", TOPIC);

            // Try to read some messages
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            log.info("Found {} messages in topic", records.count());
            
            if (records.count() > 0) {
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Message: key={}, value={}, partition={}, offset={}",
                            record.key(),
                            record.value(),
                            record.partition(),
                            record.offset());
                }
            }
        }
    }
} 