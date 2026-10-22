package com.hector.ticketing.commons.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.hector.ticketing.commons.domain.DomainValidationException;
import com.hector.ticketing.commons.domain.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Standalone MockMvc: only a test controller and the advices, no Spring Boot context. */
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    // Registered in the "wrong" order on purpose: DataIntegrityExceptionHandler must still win
    // over the global catch-all thanks to its Ordered precedence.
    private final MockMvcTester mvc = MockMvcTester.create(MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler(), new DataIntegrityExceptionHandler())
            .build());

    @Test
    void notFoundReturns404AndLogsWarn(CapturedOutput output) {
        MvcTestResult result = mvc.get().uri("/not-found").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Thing 42 not found");
        assertThat(output).contains("WARN").contains("Client error 404 on uri=/not-found");
    }

    @Test
    void domainValidationReturns400() {
        MvcTestResult result = mvc.get().uri("/invalid").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Thing name is required");
    }

    @Test
    void dataIntegrityViolationReturns409WithoutDatabaseDetails(CapturedOutput output) {
        MvcTestResult result = mvc.get().uri("/conflict").exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Conflict");
        assertThat(result).bodyText().doesNotContain("fk_thing_owner");
        assertThat(output).contains("Client error 409").contains("fk_thing_owner");
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalsAndLogsStackTrace(CapturedOutput output) {
        MvcTestResult result = mvc.get().uri("/boom").exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Internal server error");
        assertThat(result).bodyText().doesNotContain("secret").doesNotContain("IllegalStateException");
        assertThat(output)
                .contains("ERROR")
                .contains("Unexpected error on uri=/boom")
                .contains("java.lang.IllegalStateException: connection password=secret")
                .contains("at com.hector.ticketing.commons.web.GlobalExceptionHandlerTest$FailingController");
    }

    @Test
    void springMvcErrorsAreStillHandledByTheBaseClass(CapturedOutput output) {
        MvcTestResult result = mvc.post().uri("/not-found").exchange();

        assertThat(result).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(output).contains("Client error 405");
    }

    @RestController
    static class FailingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ThingNotFoundException();
        }

        @GetMapping("/invalid")
        void invalid() {
            throw new DomainValidationException("Thing name is required");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new DataIntegrityViolationException(
                    "insert or update on table \"thing\" violates foreign key constraint \"fk_thing_owner\"");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("connection password=secret");
        }
    }

    static class ThingNotFoundException extends NotFoundException {
        ThingNotFoundException() {
            super("Thing 42 not found");
        }
    }
}
