package com.paklog.wes.tracking.domain.entity;

import com.paklog.wes.tracking.domain.valueobject.MovementType;
import java.time.LocalDateTime;

/**
 * Movement - Record of physical movement between locations
 */
public class Movement {
    private String movementId;
    private MovementType type;
    private String fromLocationId;
    private String toLocationId;
    private String performedBy; // Worker/user ID
    private LocalDateTime occurredAt;
    private String reason;
    private String taskId; // Associated task
    private String waveId; // Associated wave

    protected Movement() {
        // For persistence
    }

    public Movement(MovementType type, String fromLocationId, String toLocationId,
                   String performedBy, String reason) {
        this.movementId = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.fromLocationId = fromLocationId;
        this.toLocationId = toLocationId;
        this.performedBy = performedBy;
        this.occurredAt = LocalDateTime.now();
        this.reason = reason;
    }

    public static Movement create(MovementType type, String fromLocationId, String toLocationId,
                                 String performedBy, String reason) {
        if (type == null) {
            throw new IllegalArgumentException("Movement type is required");
        }

        return new Movement(type, fromLocationId, toLocationId, performedBy, reason);
    }

    /**
     * Associate with task
     */
    public void associateWithTask(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Associate with wave
     */
    public void associateWithWave(String waveId) {
        this.waveId = waveId;
    }

    /**
     * Check if this is an inbound movement (adds to location)
     */
    public boolean isInbound() {
        return type.addsInventory();
    }

    /**
     * Check if this is an outbound movement (removes from location)
     */
    public boolean isOutbound() {
        return type.removesInventory();
    }

    /**
     * Get movement duration from another movement
     */
    public long getTimeSinceMovement(Movement previousMovement) {
        if (previousMovement == null) {
            return 0;
        }
        return java.time.Duration.between(previousMovement.occurredAt, this.occurredAt).getSeconds();
    }

    // Getters
    public String getMovementId() {
        return movementId;
    }

    public MovementType getType() {
        return type;
    }

    public String getFromLocationId() {
        return fromLocationId;
    }

    public String getToLocationId() {
        return toLocationId;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getReason() {
        return reason;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getWaveId() {
        return waveId;
    }

    @Override
    public String toString() {
        return String.format("Movement[type=%s, from=%s, to=%s, at=%s]",
            type, fromLocationId, toLocationId, occurredAt);
    }
}
