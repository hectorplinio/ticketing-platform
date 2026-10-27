package com.hector.ticketing.catalog.infrastructure.input.rest.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.hector.ticketing.catalog.TestcontainersConfiguration;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class EventControllerTest {

    private static final String START = "2026-12-01T20:00:00Z";
    private static final String END = "2026-12-01T22:00:00Z";

    @Autowired
    private MockMvcTester mvc;

    // Every event needs an existing venue; each test gets its own so they stay independent
    private String venueId;

    @BeforeEach
    void createVenue() {
        MvcTestResult result = mvc.post()
                .uri("/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"WiZink Center\", \"address\": \"Av. Felipe II, Madrid\", \"capacity\": 15000}")
                .exchange();
        String path = locationOf(result);
        venueId = path.substring(path.lastIndexOf('/') + 1);
    }

    @Test
    void createReturns201WithLocationAndBody() {
        MvcTestResult result = create(venueId, "Rock Fest", START, END);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result.getResponse().getHeader("Location")).matches(".*/events/[0-9a-f-]{36}");
        assertThat(result).bodyJson().extractingPath("$.venueId").isEqualTo(venueId);
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Rock Fest");
        assertThat(result).bodyJson().extractingPath("$.startsAt").isEqualTo(START);
        assertThat(result).bodyJson().extractingPath("$.endsAt").isEqualTo(END);
    }

    @Test
    void getReturnsCreatedEvent() {
        String path = locationOf(create(venueId, "Jazz Night", START, END));

        MvcTestResult result = mvc.get().uri(path).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Jazz Night");
        assertThat(result).bodyJson().extractingPath("$.description").isEqualTo("Live music");
    }

    @Test
    void listContainsCreatedEvent() {
        create(venueId, "Opera Gala", START, END);

        MvcTestResult result = mvc.get().uri("/events").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[*].name").asArray().contains("Opera Gala");
    }

    @Test
    void updateChangesEventAndKeepsVenue() {
        String path = locationOf(create(venueId, "Old name", START, END));

        MvcTestResult result = mvc.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "New name", "startsAt": "2026-12-02T20:00:00Z", "endsAt": "2026-12-02T23:00:00Z"}
                        """)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("New name");
        assertThat(result).bodyJson().extractingPath("$.description").isNull();
        assertThat(result).bodyJson().extractingPath("$.venueId").isEqualTo(venueId);
        assertThat(mvc.get().uri(path).exchange())
                .bodyJson()
                .extractingPath("$.endsAt")
                .isEqualTo("2026-12-02T23:00:00Z");
    }

    @Test
    void deleteRemovesEvent() {
        String path = locationOf(create(venueId, "To delete", START, END));

        assertThat(mvc.delete().uri(path).exchange()).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(path).exchange()).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownEventReturns404ProblemDetail() {
        MvcTestResult result = mvc.get().uri("/events/{id}", UUID.randomUUID()).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void createWithUnknownVenueReturns404() {
        MvcTestResult result = create(UUID.randomUUID().toString(), "Rock Fest", START, END);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void endBeforeStartReturns400() {
        MvcTestResult result = create(venueId, "Rock Fest", END, START);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Invalid request");
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() {
        MvcTestResult result = mvc.post()
                .uri("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\", \"startsAt\": \"" + START + "\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errors.venueId").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.name").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.endsAt").isNotNull();
    }

    @Test
    void deletingVenueWithEventsReturns409() {
        create(venueId, "Rock Fest", START, END);

        MvcTestResult result = mvc.delete().uri("/venues/{id}", venueId).exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.get().uri("/venues/{id}", venueId).exchange()).hasStatusOk();
    }

    private MvcTestResult create(String venueId, String name, String startsAt, String endsAt) {
        return mvc.post()
                .uri("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"venueId": "%s", "name": "%s", "description": "Live music", "startsAt": "%s", "endsAt": "%s"}
                        """.formatted(venueId, name, startsAt, endsAt))
                .exchange();
    }

    private static String locationOf(MvcTestResult result) {
        return URI.create(result.getResponse().getHeader("Location")).getPath();
    }
}
