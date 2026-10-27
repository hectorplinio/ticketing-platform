package com.hector.ticketing.catalog.infrastructure.input.rest.event;

import com.hector.ticketing.catalog.application.event.CreateEvent;
import com.hector.ticketing.catalog.application.event.DeleteEvent;
import com.hector.ticketing.catalog.application.event.GetEvent;
import com.hector.ticketing.catalog.application.event.ListEvents;
import com.hector.ticketing.catalog.application.event.UpdateEvent;
import com.hector.ticketing.catalog.domain.event.Event;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Input adapter: translates HTTP into use case calls. No business logic here. */
@RestController
@RequestMapping("/events")
class EventController {

    private final CreateEvent createEvent;
    private final GetEvent getEvent;
    private final ListEvents listEvents;
    private final UpdateEvent updateEvent;
    private final DeleteEvent deleteEvent;

    EventController(
            CreateEvent createEvent,
            GetEvent getEvent,
            ListEvents listEvents,
            UpdateEvent updateEvent,
            DeleteEvent deleteEvent) {
        this.createEvent = createEvent;
        this.getEvent = getEvent;
        this.listEvents = listEvents;
        this.updateEvent = updateEvent;
        this.deleteEvent = deleteEvent;
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        Event event = createEvent.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(event.getId())
                .toUri();
        return ResponseEntity.created(location).body(EventResponse.from(event));
    }

    @GetMapping
    List<EventResponse> list() {
        return listEvents.execute().stream().map(EventResponse::from).toList();
    }

    @GetMapping("/{id}")
    EventResponse get(@PathVariable UUID id) {
        return EventResponse.from(getEvent.execute(id));
    }

    @PutMapping("/{id}")
    EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        return EventResponse.from(updateEvent.execute(request.toCommand(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        deleteEvent.execute(id);
    }
}
