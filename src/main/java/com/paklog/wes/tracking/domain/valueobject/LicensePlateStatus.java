package com.paklog.wes.tracking.domain.valueobject;

/**
 * License Plate Status - Lifecycle status of a license plate
 */
public enum LicensePlateStatus {
    CREATED("License plate created, not yet in use"),
    ACTIVE("Active and in use"),
    IN_TRANSIT("Moving between locations"),
    AT_LOCATION("Stationary at a location"),
    PICKED("Being picked"),
    PACKED("Packed for shipping"),
    SHIPPED("Shipped out of warehouse"),
    CONSUMED("Contents consumed, LP emptied"),
    CLOSED("Closed and archived");

    private final String description;

    LicensePlateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if license plate can be moved
     */
    public boolean canBeMoved() {
        return switch (this) {
            case ACTIVE, AT_LOCATION, IN_TRANSIT -> true;
            case CREATED, PICKED, PACKED, SHIPPED, CONSUMED, CLOSED -> false;
        };
    }

    /**
     * Check if license plate can have items added
     */
    public boolean canAddItems() {
        return switch (this) {
            case CREATED, ACTIVE, AT_LOCATION -> true;
            case IN_TRANSIT, PICKED, PACKED, SHIPPED, CONSUMED, CLOSED -> false;
        };
    }

    /**
     * Check if license plate can have items removed
     */
    public boolean canRemoveItems() {
        return switch (this) {
            case ACTIVE, AT_LOCATION, PICKED -> true;
            case CREATED, IN_TRANSIT, PACKED, SHIPPED, CONSUMED, CLOSED -> false;
        };
    }

    /**
     * Check if license plate is in final state
     */
    public boolean isFinalState() {
        return this == SHIPPED || this == CONSUMED || this == CLOSED;
    }
}
