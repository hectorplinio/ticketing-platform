package com.hector.ticketing.commons.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Registers {@link KafkaHealthIndicator} in every service that uses Kafka, with no code in the
 * service: adding the platform-commons dependency is enough.
 *
 * <p>Runs after Boot's Kafka auto-configuration so that its {@link KafkaAdmin} bean already exists.
 */
@AutoConfiguration(afterName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@ConditionalOnClass({KafkaAdmin.class, HealthIndicator.class})
public class KafkaHealthAutoConfiguration {

    // Boot derives the health contributor name from the bean name: "kafkaHealthIndicator" -> "kafka",
    // which is the name the readiness group includes. Do not rename this method.
    @Bean
    @ConditionalOnBean(KafkaAdmin.class)
    @ConditionalOnMissingBean(name = "kafkaHealthIndicator")
    KafkaHealthIndicator kafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        return new KafkaHealthIndicator(kafkaAdmin);
    }
}
