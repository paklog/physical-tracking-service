package com.paklog.wes.tracking.application.service;

import com.paklog.wes.tracking.adapter.event.TrackingEventPublisher;
import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.repository.LicensePlateRepository;
import com.paklog.wes.tracking.domain.repository.LocationStateRepository;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application service for physical tracking operations
 */
@Service
@Transactional
public class PhysicalTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(PhysicalTrackingService.class);

    private final LicensePlateRepository licensePlateRepository;
    private final LocationStateRepository locationStateRepository;
    private final TrackingEventPublisher eventPublisher;

    public PhysicalTrackingService(
            LicensePlateRepository licensePlateRepository,
            LocationStateRepository locationStateRepository,
            TrackingEventPublisher eventPublisher
    ) {
        this.licensePlateRepository = licensePlateRepository;
        this.locationStateRepository = locationStateRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new license plate
     */
    public LicensePlate createLicensePlate(
            String licensePlateId,
            String warehouseId,
            LicensePlateType type,
            String containerCode,
            String createdBy
    ) {
        logger.info("Creating license plate: {} in warehouse {}", licensePlateId, warehouseId);

        LicensePlate lp = LicensePlate.create(
            licensePlateId, warehouseId, type, containerCode, createdBy
        );

        lp = licensePlateRepository.save(lp);

        // Publish event
        eventPublisher.publishLicensePlateCreated(
            licensePlateId, warehouseId, type.name(), createdBy
        );

        return lp;
    }

    /**
     * Get license plate by ID
     */
    @Transactional(readOnly = true)
    public Optional<LicensePlate> getLicensePlate(String licensePlateId) {
        return licensePlateRepository.findById(licensePlateId);
    }

    /**
     * Move license plate to location
     */
    public LicensePlate moveLicensePlate(
            String licensePlateId,
            String toLocationId,
            MovementType movementType,
            String performedBy,
            String reason
    ) {
        logger.info("Moving license plate {} to location {}", licensePlateId, toLocationId);

        LicensePlate lp = getLicensePlateOrThrow(licensePlateId);
        String fromLocationId = lp.getCurrentLocationId();

        // Remove from previous location state
        if (fromLocationId != null) {
            LocationState fromState = getOrCreateLocationState(fromLocationId, lp.getWarehouseId(), null);
            fromState.removeLicensePlate(
                licensePlateId,
                lp.getTotalQuantity(),
                lp.getTotalWeight(),
                lp.getTotalVolume()
            );
            locationStateRepository.save(fromState);
        }

        // Move license plate
        lp.moveTo(toLocationId, movementType, performedBy, reason);
        lp = licensePlateRepository.save(lp);

        // Add to new location state
        if (toLocationId != null) {
            LocationState toState = getOrCreateLocationState(toLocationId, lp.getWarehouseId(), null);
            toState.addLicensePlate(
                licensePlateId,
                lp.getTotalQuantity(),
                lp.getTotalWeight(),
                lp.getTotalVolume()
            );
            locationStateRepository.save(toState);
        }

        // Publish event
        eventPublisher.publishLicensePlateMoved(
            licensePlateId, fromLocationId, toLocationId, movementType.name(), performedBy
        );

        return lp;
    }

    /**
     * Add item to license plate
     */
    public LicensePlate addItemToLicensePlate(
            String licensePlateId,
            String sku,
            String lotNumber,
            Integer quantity,
            BigDecimal weight,
            BigDecimal volume,
            String uom
    ) {
        logger.info("Adding item {} (qty={}) to license plate {}", sku, quantity, licensePlateId);

        LicensePlate lp = getLicensePlateOrThrow(licensePlateId);
        lp.addItem(sku, lotNumber, quantity, weight, volume, uom);

        // Update location state if at location
        if (lp.getCurrentLocationId() != null) {
            LocationState state = getOrCreateLocationState(
                lp.getCurrentLocationId(), lp.getWarehouseId(), null
            );
            // Recalculate by removing and re-adding
            state.removeLicensePlate(licensePlateId, 0, BigDecimal.ZERO, BigDecimal.ZERO);
            state.addLicensePlate(
                licensePlateId,
                lp.getTotalQuantity(),
                lp.getTotalWeight(),
                lp.getTotalVolume()
            );
            locationStateRepository.save(state);
        }

        lp = licensePlateRepository.save(lp);

        // Publish event
        eventPublisher.publishItemAdded(
            licensePlateId, sku, quantity, lp.getCurrentLocationId()
        );

        return lp;
    }

    /**
     * Remove item from license plate
     */
    public LicensePlate removeItemFromLicensePlate(
            String licensePlateId,
            String sku,
            String lotNumber,
            Integer quantity
    ) {
        logger.info("Removing item {} (qty={}) from license plate {}", sku, quantity, licensePlateId);

        LicensePlate lp = getLicensePlateOrThrow(licensePlateId);
        lp.removeItem(sku, lotNumber, quantity);

        // Update location state if at location
        if (lp.getCurrentLocationId() != null) {
            LocationState state = getOrCreateLocationState(
                lp.getCurrentLocationId(), lp.getWarehouseId(), null
            );
            state.removeLicensePlate(licensePlateId, 0, BigDecimal.ZERO, BigDecimal.ZERO);
            if (!lp.isEmpty()) {
                state.addLicensePlate(
                    licensePlateId,
                    lp.getTotalQuantity(),
                    lp.getTotalWeight(),
                    lp.getTotalVolume()
                );
            }
            locationStateRepository.save(state);
        }

        lp = licensePlateRepository.save(lp);

        // Publish event
        eventPublisher.publishItemRemoved(
            licensePlateId, sku, quantity, lp.getCurrentLocationId()
        );

        return lp;
    }

    /**
     * Block location
     */
    public LocationState blockLocation(String locationId, String reason) {
        logger.info("Blocking location {}: {}", locationId, reason);

        LocationState state = locationStateRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location state not found: " + locationId));

        state.block(reason);
        state = locationStateRepository.save(state);

        // Publish event
        eventPublisher.publishLocationBlocked(
            locationId, state.getWarehouseId(), reason
        );

        return state;
    }

    /**
     * Unblock location
     */
    public LocationState unblockLocation(String locationId) {
        logger.info("Unblocking location {}", locationId);

        LocationState state = locationStateRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location state not found: " + locationId));

        state.unblock();
        state = locationStateRepository.save(state);

        // Publish event
        eventPublisher.publishLocationUnblocked(
            locationId, state.getWarehouseId()
        );

        return state;
    }

    /**
     * Get license plates at location
     */
    @Transactional(readOnly = true)
    public List<LicensePlate> getLicensePlatesAtLocation(String locationId) {
        return licensePlateRepository.findByCurrentLocationId(locationId);
    }

    /**
     * Get location state
     */
    @Transactional(readOnly = true)
    public LocationState getLocationState(String locationId) {
        return locationStateRepository.findById(locationId)
            .orElse(null);
    }

    /**
     * Get all location states for warehouse
     */
    @Transactional(readOnly = true)
    public List<LocationState> getLocationStates(String warehouseId) {
        return locationStateRepository.findByWarehouseId(warehouseId);
    }

    private LicensePlate getLicensePlateOrThrow(String licensePlateId) {
        return licensePlateRepository.findById(licensePlateId)
            .orElseThrow(() -> new IllegalArgumentException(
                "License plate not found: " + licensePlateId));
    }

    private LocationState getOrCreateLocationState(String locationId, String warehouseId, String zone) {
        return locationStateRepository.findById(locationId)
            .orElseGet(() -> LocationState.create(
                locationId, warehouseId, zone, 1000,
                new BigDecimal("1000"), new BigDecimal("10")
            ));
    }
}
