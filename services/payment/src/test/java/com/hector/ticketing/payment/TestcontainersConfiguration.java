package com.hector.ticketing.payment;

import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    // Images pinned to the same versions as infra/docker-compose.yml, so tests run against what we develop with.
    // Generous startup timeout: in CI every service starts its own containers in parallel on a small runner.
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.3.1"))
                .withStartupTimeout(STARTUP_TIMEOUT);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17")).withStartupTimeout(STARTUP_TIMEOUT);
    }
}
