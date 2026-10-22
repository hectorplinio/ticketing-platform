package com.hector.ticketing.commons.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Registers {@link GlobalExceptionHandler} in every servlet (Spring MVC) service, so all of them
 * return the same RFC 9457 Problem Details.
 *
 * <p>Runs before Boot's WebMvc auto-configuration: Boot only adds its own {@code
 * ResponseEntityExceptionHandler} when none exists, so ours must be registered first.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler.class)
public class ErrorHandlingAutoConfiguration {

    // A service can still replace it by declaring its own ResponseEntityExceptionHandler bean.
    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
    GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    // Nested class so the condition is evaluated before DataIntegrityExceptionHandler is loaded:
    // services without spring-tx (no database) never touch it.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.dao.DataIntegrityViolationException")
    static class DataIntegrityConfiguration {

        @Bean
        DataIntegrityExceptionHandler dataIntegrityExceptionHandler() {
            return new DataIntegrityExceptionHandler();
        }
    }
}
