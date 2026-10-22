package com.hector.ticketing.commons.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaAdmin;

/** Checks only the auto-configuration conditions: no broker is needed to create the AdminClient. */
class KafkaHealthAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(KafkaHealthAutoConfiguration.class));

    @Test
    void registersKafkaHealthIndicatorWhenKafkaAdminExists() {
        runner.withBean(
                        KafkaAdmin.class,
                        () -> new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")))
                .run(context -> assertThat(context)
                        .hasSingleBean(KafkaHealthIndicator.class)
                        .hasBean("kafkaHealthIndicator"));
    }

    @Test
    void backsOffWithoutKafkaAdmin() {
        runner.run(context -> assertThat(context).doesNotHaveBean(KafkaHealthIndicator.class));
    }
}
