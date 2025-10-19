package com.paklog.wes.tracking.domain.valueobject;

/**
 * Occupancy Status - Physical occupancy state of a location
 */
public enum OccupancyStatus {
    EMPTY("No inventory in location"),
    PARTIALLY_OCCUPIED("Some capacity available"),
    FULL("At or near maximum capacity"),
    OVER_CAPACITY("Exceeds maximum capacity"),
    BLOCKED("Physically blocked or inaccessible"),
    UNKNOWN("Occupancy state unknown");

    private final String description;

    OccupancyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if location can accept more inventory
     */
    public boolean canAcceptInventory() {
        return this == EMPTY || this == PARTIALLY_OCCUPIED;
    }

    /**
     * Check if location requires attention
     */
    public boolean requiresAttention() {
        return this == OVER_CAPACITY || this == BLOCKED;
    }

    /**
     * Determine occupancy status from utilization percentage
     */
    public static OccupancyStatus fromUtilization(double utilizationPct) {
        if (utilizationPct < 0) {
            return UNKNOWN;
        } else if (utilizationPct == 0) {
            return EMPTY;
        } else if (utilizationPct < 95) {
            return PARTIALLY_OCCUPIED;
        } else if (utilizationPct <= 100) {
            return FULL;
        } else {
            return OVER_CAPACITY;
        }
    }
}
