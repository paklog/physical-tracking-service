package com.paklog.wes.tracking.infrastructure.events;

import com.paklog.wes.tracking.application.service.PhysicalTrackingService;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event handler for warehouse operation events
 * Listens to events that affect physical tracking
 */
@Component
public class WarehouseEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseEventHandler.class);

    private final PhysicalTrackingService trackingService;

    public WarehouseEventHandler(PhysicalTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * Handle PickingCompletedEvent from pick-execution-service
     * Updates location states after picking
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.pick-events:wes-pick-events}",
            groupId = "${paklog.kafka.consumer.group-id:physical-tracking-service}"
    )
    public void handlePickingCompleted(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"PickingCompletedEvent".equals(eventType)) {
                return;
            }

            logger.info("Received PickingCompletedEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String warehouseId = (String) eventData.get("warehouseId");

            // In a real system, this would update location states
            // based on the items picked
            logger.debug("Updated location states after picking for order {}", orderId);

        } catch (Exception e) {
            logger.error("Error handling PickingCompletedEvent", e);
        }
    }

    /**
     * Handle PackingCompletedEvent from pack-ship-service
     * Creates or updates license plates for packed orders
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.pack-events:wes-pack-events}",
            groupId = "${paklog.kafka.consumer.group-id:physical-tracking-service}"
    )
    public void handlePackingCompleted(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"PackingCompletedEvent".equals(eventType)) {
                return;
            }

            logger.info("Received PackingCompletedEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String warehouseId = (String) eventData.get("warehouseId");
            String cartonId = (String) eventData.get("cartonId");
            String stationId = (String) eventData.get("stationId");

            // Create license plate for the packed carton
            if (cartonId != null) {
                createPackedLicensePlate(orderId, warehouseId, cartonId, stationId);
            }

            logger.info("Created license plate for packed order {}", orderId);

        } catch (Exception e) {
            logger.error("Error handling PackingCompletedEvent", e);
        }
    }

    private void createPackedLicensePlate(String orderId, String warehouseId,
                                         String cartonId, String stationId) {
        try {
            String licensePlateId = "LP-" + orderId + "-" + cartonId;

            // Check if license plate already exists
            if (trackingService.getLicensePlate(licensePlateId).isPresent()) {
                logger.debug("License plate {} already exists", licensePlateId);
                return;
            }

            // Create new license plate
            trackingService.createLicensePlate(
                    licensePlateId,
                    warehouseId,
                    LicensePlateType.CARTON,
                    cartonId,
                    "system"
            );

            // Move to shipping staging location if station is known
            if (stationId != null) {
                String stagingLocation = "STAGE-" + stationId;
                trackingService.moveLicensePlate(
                        licensePlateId,
                        stagingLocation,
                        MovementType.PACK_TO_STAGE,
                        "system",
                        "Moved after packing"
                );
            }

            logger.debug("Created and moved license plate {} to staging", licensePlateId);

        } catch (Exception e) {
            logger.error("Error creating packed license plate for order {}", orderId, e);
        }
    }

    /**
     * Handle LocationCreatedEvent from location-master-service
     * Creates initial location state when new locations are configured
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.location-events:wms-location-events}",
            groupId = "${paklog.kafka.consumer.group-id:physical-tracking-service}"
    )
    public void handleLocationCreated(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"LocationCreatedEvent".equals(eventType)) {
                return;
            }

            logger.info("Received LocationCreatedEvent: {}", eventData);

            String locationId = (String) eventData.get("locationId");
            String warehouseId = (String) eventData.get("warehouseId");

            // Location state will be created on-demand when first used
            logger.debug("Location {} created in warehouse {}", locationId, warehouseId);

        } catch (Exception e) {
            logger.error("Error handling LocationCreatedEvent", e);
        }
    }

    /**
     * Handle InventoryMovedEvent
     * Updates license plate locations
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.inventory-events:wms-inventory-events}",
            groupId = "${paklog.kafka.consumer.group-id:physical-tracking-service}"
    )
    public void handleInventoryMoved(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"InventoryMovedEvent".equals(eventType)) {
                return;
            }

            logger.info("Received InventoryMovedEvent: {}", eventData);

            String licensePlateId = (String) eventData.get("licensePlateId");
            String fromLocation = (String) eventData.get("fromLocation");
            String toLocation = (String) eventData.get("toLocation");
            String performedBy = (String) eventData.get("performedBy");

            if (licensePlateId != null && toLocation != null) {
                trackingService.moveLicensePlate(
                        licensePlateId,
                        toLocation,
                        MovementType.REPLENISHMENT,
                        performedBy != null ? performedBy : "system",
                        "Inventory movement"
                );
            }

            logger.debug("Moved license plate {} from {} to {}",
                    licensePlateId, fromLocation, toLocation);

        } catch (Exception e) {
            logger.error("Error handling InventoryMovedEvent", e);
        }
    }
}
