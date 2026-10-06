package com.banque.order.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Création des topics Kafka au démarrage (3 partitions, 1 réplique car un seul broker en local). */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderRequestsTopic(@Value("${app.kafka.topics.order-requests}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedTopic(@Value("${app.kafka.topics.order-created}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
