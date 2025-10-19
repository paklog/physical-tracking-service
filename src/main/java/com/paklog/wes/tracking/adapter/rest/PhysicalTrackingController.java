package com.paklog.wes.tracking.adapter.rest;

import com.paklog.wes.tracking.adapter.rest.dto.*;
import com.paklog.wes.tracking.application.service.PhysicalTrackingService;
import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.aggregate.LocationState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for physical tracking operations
 */
@RestController
@RequestMapping("/api/v1/tracking")
@Tag(name = "Physical Tracking", description = "License plate and location state tracking")
public class PhysicalTrackingController {

    private final PhysicalTrackingService trackingService;

    public PhysicalTrackingController(PhysicalTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * Create a new license plate
     */
    @PostMapping("/license-plates")
    @Operation(summary = "Create license plate", description = "Create a new license plate")
    public ResponseEntity<LicensePlateResponse> createLicensePlate(
            @Valid @RequestBody CreateLicensePlateRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LicensePlate lp = trackingService.createLicensePlate(
            request.licensePlateId(),
            request.warehouseId(),
            request.type(),
            request.containerCode(),
            userId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(LicensePlateResponse.from(lp));
    }

    /**
     * Get license plate by ID
     */
    @GetMapping("/license-plates/{id}")
    @Operation(summary = "Get license plate", description = "Get license plate details by ID")
    public ResponseEntity<LicensePlateResponse> getLicensePlate(@PathVariable String id) {
        return trackingService.getLicensePlate(id)
            .map(LicensePlateResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List license plates at a location
     */
    @GetMapping("/license-plates")
    @Operation(summary = "List license plates", description = "List license plates by location")
    public ResponseEntity<List<LicensePlateResponse>> listLicensePlates(
            @RequestParam(required = false) String locationId
    ) {
        List<LicensePlate> licensePlates;

        if (locationId != null) {
            licensePlates = trackingService.getLicensePlatesAtLocation(locationId);
        } else {
            licensePlates = List.of(); // In production, would support warehouse-level queries
        }

        return ResponseEntity.ok(
            licensePlates.stream()
                .map(LicensePlateResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Move license plate to a new location
     */
    @PostMapping("/license-plates/{id}/move")
    @Operation(summary = "Move license plate", description = "Move license plate to a new location")
    public ResponseEntity<LicensePlateResponse> moveLicensePlate(
            @PathVariable String id,
            @Valid @RequestBody MoveLicensePlateRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LicensePlate lp = trackingService.moveLicensePlate(
            id,
            request.toLocationId(),
            request.movementType(),
            userId,
            request.reason()
        );

        return ResponseEntity.ok(LicensePlateResponse.from(lp));
    }

    /**
     * Add item to license plate
     */
    @PostMapping("/license-plates/{id}/items")
    @Operation(summary = "Add item", description = "Add item to license plate")
    public ResponseEntity<LicensePlateResponse> addItem(
            @PathVariable String id,
            @Valid @RequestBody AddItemRequest request
    ) {
        LicensePlate lp = trackingService.addItemToLicensePlate(
            id,
            request.sku(),
            request.lotNumber(),
            request.quantity(),
            request.weight(),
            request.volume(),
            request.uom()
        );

        return ResponseEntity.ok(LicensePlateResponse.from(lp));
    }

    /**
     * Remove item from license plate
     */
    @DeleteMapping("/license-plates/{id}/items")
    @Operation(summary = "Remove item", description = "Remove item from license plate")
    public ResponseEntity<LicensePlateResponse> removeItem(
            @PathVariable String id,
            @RequestParam String sku,
            @RequestParam(required = false) String lotNumber,
            @RequestParam Integer quantity
    ) {
        LicensePlate lp = trackingService.removeItemFromLicensePlate(
            id,
            sku,
            lotNumber,
            quantity
        );

        return ResponseEntity.ok(LicensePlateResponse.from(lp));
    }

    /**
     * Get location state
     */
    @GetMapping("/locations/{id}/state")
    @Operation(summary = "Get location state", description = "Get current location state and occupancy")
    public ResponseEntity<LocationStateResponse> getLocationState(@PathVariable String id) {
        LocationState state = trackingService.getLocationState(id);

        if (state == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(LocationStateResponse.from(state));
    }

    /**
     * List location states
     */
    @GetMapping("/locations")
    @Operation(summary = "List location states", description = "List location states for warehouse")
    public ResponseEntity<List<LocationStateResponse>> listLocationStates(
            @RequestParam String warehouseId
    ) {
        List<LocationState> states = trackingService.getLocationStates(warehouseId);

        return ResponseEntity.ok(
            states.stream()
                .map(LocationStateResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Block location
     */
    @PostMapping("/locations/{id}/block")
    @Operation(summary = "Block location", description = "Block a location")
    public ResponseEntity<LocationStateResponse> blockLocation(
            @PathVariable String id,
            @RequestParam String reason
    ) {
        LocationState state = trackingService.blockLocation(id, reason);
        return ResponseEntity.ok(LocationStateResponse.from(state));
    }

    /**
     * Unblock location
     */
    @PostMapping("/locations/{id}/unblock")
    @Operation(summary = "Unblock location", description = "Unblock a location")
    public ResponseEntity<LocationStateResponse> unblockLocation(@PathVariable String id) {
        LocationState state = trackingService.unblockLocation(id);
        return ResponseEntity.ok(LocationStateResponse.from(state));
    }
}
