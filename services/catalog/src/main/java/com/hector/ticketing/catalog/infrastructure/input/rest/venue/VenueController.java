package com.hector.ticketing.catalog.infrastructure.input.rest.venue;

import com.hector.ticketing.catalog.application.venue.CreateVenue;
import com.hector.ticketing.catalog.application.venue.DeleteVenue;
import com.hector.ticketing.catalog.application.venue.GetVenue;
import com.hector.ticketing.catalog.application.venue.ListVenues;
import com.hector.ticketing.catalog.application.venue.UpdateVenue;
import com.hector.ticketing.catalog.domain.venue.Venue;
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
@RequestMapping("/venues")
class VenueController {

    private final CreateVenue createVenue;
    private final GetVenue getVenue;
    private final ListVenues listVenues;
    private final UpdateVenue updateVenue;
    private final DeleteVenue deleteVenue;

    VenueController(
            CreateVenue createVenue,
            GetVenue getVenue,
            ListVenues listVenues,
            UpdateVenue updateVenue,
            DeleteVenue deleteVenue) {
        this.createVenue = createVenue;
        this.getVenue = getVenue;
        this.listVenues = listVenues;
        this.updateVenue = updateVenue;
        this.deleteVenue = deleteVenue;
    }

    @PostMapping
    ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest request) {
        Venue venue = createVenue.execute(request.toCreateCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(venue.getId())
                .toUri();
        return ResponseEntity.created(location).body(VenueResponse.from(venue));
    }

    @GetMapping
    List<VenueResponse> list() {
        return listVenues.execute().stream().map(VenueResponse::from).toList();
    }

    @GetMapping("/{id}")
    VenueResponse get(@PathVariable UUID id) {
        return VenueResponse.from(getVenue.execute(id));
    }

    @PutMapping("/{id}")
    VenueResponse update(@PathVariable UUID id, @Valid @RequestBody VenueRequest request) {
        return VenueResponse.from(updateVenue.execute(request.toUpdateCommand(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        deleteVenue.execute(id);
    }
}
