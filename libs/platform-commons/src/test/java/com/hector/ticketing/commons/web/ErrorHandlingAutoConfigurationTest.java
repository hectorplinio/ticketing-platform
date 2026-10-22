package com.hector.ticketing.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

class ErrorHandlingAutoConfigurationTest {

    private static final AutoConfigurations CONFIG = AutoConfigurations.of(ErrorHandlingAutoConfiguration.class);

    @Test
    void registersGlobalExceptionHandlerInServletApps() {
        new WebApplicationContextRunner()
                .withConfiguration(CONFIG)
                .run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void registersDataIntegrityHandlerWhenSpringTxIsPresent() {
        new WebApplicationContextRunner()
                .withConfiguration(CONFIG)
                .run(context -> assertThat(context).hasSingleBean(DataIntegrityExceptionHandler.class));
    }

    @Test
    void skipsDataIntegrityHandlerWithoutSpringTx() {
        // Simulates a service with no database (notification): spring-tx is not on the classpath.
        new WebApplicationContextRunner()
                .withConfiguration(CONFIG)
                .withClassLoader(new FilteredClassLoader(DataIntegrityViolationException.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(GlobalExceptionHandler.class)
                        .doesNotHaveBean(DataIntegrityExceptionHandler.class));
    }

    @Test
    void backsOffOutsideWebApps() {
        new ApplicationContextRunner()
                .withConfiguration(CONFIG)
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Test
    void backsOffWhenServiceDefinesItsOwnHandler() {
        new WebApplicationContextRunner()
                .withConfiguration(CONFIG)
                .withBean(ResponseEntityExceptionHandler.class, () -> new ResponseEntityExceptionHandler() {})
                .run(context -> assertThat(context)
                        .hasSingleBean(ResponseEntityExceptionHandler.class)
                        .doesNotHaveBean(GlobalExceptionHandler.class));
    }
}
