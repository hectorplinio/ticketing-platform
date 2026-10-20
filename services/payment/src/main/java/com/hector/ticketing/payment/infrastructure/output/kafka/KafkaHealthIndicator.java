package com.hector.ticketing.payment.infrastructure.output.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Spring Boot has no built-in Kafka health indicator. Contributes as "kafka"; the timeout
 * stays below the gateway's 2 s response timeout so a broker outage reports DOWN instead of 504.
 */
@Component
class KafkaHealthIndicator implements HealthIndicator, AutoCloseable {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final AdminClient adminClient;

    KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());
        this.adminClient = AdminClient.create(config);
    }

    @Override
    public Health health() {
        try {
            adminClient
                    .describeCluster(new DescribeClusterOptions().timeoutMs((int) TIMEOUT.toMillis()))
                    .clusterId()
                    .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return Health.up().build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down(e).build();
        } catch (ExecutionException | TimeoutException e) {
            return Health.down(e).build();
        }
    }

    @Override
    public void close() {
        adminClient.close(TIMEOUT);
    }
}
