package com.hector.ticketing.catalog.infrastructure.input.rest.venue;

import static org.assertj.core.api.Assertions.assertThat;

import com.hector.ticketing.catalog.TestcontainersConfiguration;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * End-to-end through the whole service: HTTP -> controller -> use case -> adapter -> real Postgres
 * (Testcontainers) with the Flyway migrations applied.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VenueControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void createReturns201WithLocationAndBody() {
        MvcTestResult result = create("WiZink Center", "Av. Felipe II, Madrid", 15000);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result.getResponse().getHeader("Location")).matches(".*/venues/[0-9a-f-]{36}");
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("WiZink Center");
        assertThat(result).bodyJson().extractingPath("$.capacity").isEqualTo(15000);
    }

    @Test
    void getReturnsCreatedVenue() {
        String path = locationOf(create("Teatro Real", "Plaza de Isabel II, Madrid", 1700));

        MvcTestResult result = mvc.get().uri(path).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Teatro Real");
        assertThat(result).bodyJson().extractingPath("$.address").isEqualTo("Plaza de Isabel II, Madrid");
    }

    @Test
    void listContainsCreatedVenue() {
        create("Palau Sant Jordi", "Passeig Olímpic, Barcelona", 17000);

        MvcTestResult result = mvc.get().uri("/venues").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[*].name").asArray().contains("Palau Sant Jordi");
    }

    @Test
    void updateChangesVenue() {
        String path = locationOf(create("Old name", "Old address", 100));

        MvcTestResult result = mvc.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("New name", "New address", 200))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("New name");
        assertThat(mvc.get().uri(path).exchange())
                .bodyJson()
                .extractingPath("$.capacity")
                .isEqualTo(200);
    }

    @Test
    void deleteRemovesVenue() {
        String path = locationOf(create("To delete", "Somewhere", 10));

        assertThat(mvc.delete().uri(path).exchange()).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(path).exchange()).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownVenueReturns404ProblemDetail() {
        MvcTestResult result = mvc.get().uri("/venues/{id}", UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() {
        MvcTestResult result = mvc.post()
                .uri("/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\", \"address\": \"Somewhere\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errors.name").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.capacity").isNotNull();
    }

    @Test
    void malformedIdReturns400() {
        assertThat(mvc.get().uri("/venues/not-a-uuid").exchange()).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MvcTestResult create(String name, String address, int capacity) {
        return mvc.post()
                .uri("/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(name, address, capacity))
                .exchange();
    }

    private static String locationOf(MvcTestResult result) {
        return URI.create(result.getResponse().getHeader("Location")).getPath();
    }

    private static String json(String name, String address, int capacity) {
        return """
                {"name": "%s", "address": "%s", "capacity": %d}
                """.formatted(name, address, capacity);
    }
}
