package com.paklog.wes.tracking.adapter.event;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event publisher for Physical Tracking Service
 * Publishes CloudEvents to Kafka for downstream consumption
 */
@Component
public class TrackingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TrackingEventPublisher.class);
    private static final String SOURCE = "physical-tracking-service";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;

    public TrackingEventPublisher(KafkaTemplate<String, CloudEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish license plate created event
     */
    public void publishLicensePlateCreated(
            String licensePlateId,
            String warehouseId,
            String type,
            String createdBy
    ) {
        Map<String, Object> data = Map.of(
            "licensePlateId", licensePlateId,
            "warehouseId", warehouseId,
            "type", type,
            "createdBy", createdBy
        );

        CloudEvent event = buildEvent(
            "com.paklog.tracking.licenseplate.created",
            licensePlateId,
            data
        );

        publishEvent("tracking-events", licensePlateId, event);
    }

    /**
     * Publish license plate moved event
     */
    public void publishLicensePlateMoved(
            String licensePlateId,
            String fromLocationId,
            String toLocationId,
            String movementType,
            String performedBy
    ) {
        Map<String, Object> data = Map.of(
            "licensePlateId", licensePlateId,
            "fromLocationId", fromLocationId != null ? fromLocationId : "",
            "toLocationId", toLocationId,
            "movementType", movementType,
            "performedBy", performedBy
        );

        CloudEvent event = buildEvent(
            "com.paklog.tracking.licenseplate.moved",
            licensePlateId,
            data
        );

        publishEvent("tracking-events", licensePlateId, event);
    }

    /**
     * Publish item added to license plate event
     */
    public void publishItemAdded(
            String licensePlateId,
            String sku,
            Integer quantity,
            String locationId
    ) {
        Map<String, Object> data = Map.of(
            "licensePlateId", licensePlateId,
            "sku", sku,
            "quantity", quantity,
            "locationId", locationId != null ? locationId : ""
        );

        CloudEvent event = buildEvent(
            "com.paklog.tracking.item.added",
            licensePlateId,
            data
        );

        publishEvent("tracking-events", licensePlateId, event);
    }

    /**
     * Publish item removed from license plate event
     */
    public void publishItemRemoved(
            String licensePlateId,
            String sku,
            Integer quantity,
            String locationId
    ) {
        Map<String, Object> data = Map.of(
            "licensePlateId", licensePlateId,
            "sku", sku,
            "quantity", quantity,
            "locationId", locationId != null ? locationId : ""
        );

        CloudEvent event = buildEvent(
            "com.paklog.tracking.item.removed",
            licensePlateId,
            data
        );

        publishEvent("tracking-events", licensePlateId, event);
    }

    /**
     * Publish location blocked event
     */
    public void publishLocationBlocked(
            String locationId,
            String warehouseId,
            String reason
    ) {
        Map<String, Object> data = Map.of(
            "locationId", locationId,
            "warehouseId", warehouseId,
            "reason", reason
        );

        CloudEvent event = buildEvent(
            "com.paklog.tracking.location.blocked",
            locationId,
            data
        );

        publishEvent("tracking-events", locationId, event);
    }

    /**
     * Publish location unblocked event
     */
    public void publishLocationUnblocked(
            String locationId,
            String warehouseId
    ) {
        Map<String, Object> data = Map.of(
            "locationId", locationId,
            "warehouseId", warehouseId
        );

        CloudEvent event = buildEvent(
            "com.paklog.tracking.location.unblocked",
            locationId,
            data
        );

        publishEvent("tracking-events", locationId, event);
    }

    /**
     * Build CloudEvent
     */
    private CloudEvent buildEvent(String type, String subject, Map<String, Object> data) {
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(SOURCE))
            .withType(type)
            .withSubject(subject)
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData(convertToJson(data).getBytes())
            .build();
    }

    /**
     * Publish event to Kafka
     */
    private void publishEvent(String topic, String key, CloudEvent event) {
        try {
            kafkaTemplate.send(topic, key, event);
            logger.info("Published event: type={}, subject={}, topic={}",
                event.getType(), event.getSubject(), topic);
        } catch (Exception e) {
            logger.error("Failed to publish event: type={}, subject={}",
                event.getType(), event.getSubject(), e);
        }
    }

    /**
     * Convert data map to JSON string
     */
    private String convertToJson(Map<String, Object> data) {
        try {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.error("Failed to convert data to JSON", e);
            return "{}";
        }
    }
}
