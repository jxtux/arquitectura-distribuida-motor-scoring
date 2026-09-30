package com.finanscore.audit.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaDltConfig {
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, error) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        var backoff = new ExponentialBackOff(1000, 2.0);
        backoff.setMaxInterval(10_000);
        backoff.setMaxElapsedTime(30_000);
        return new DefaultErrorHandler(recoverer, backoff);
    }
}
