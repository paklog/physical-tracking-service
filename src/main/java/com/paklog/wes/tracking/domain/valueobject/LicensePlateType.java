package com.paklog.wes.tracking.domain.valueobject;

/**
 * License Plate Type - Defines the type of license plate/container
 */
public enum LicensePlateType {
    PALLET("Standard pallet"),
    TOTE("Picking tote/bin"),
    CARTON("Shipping carton/box"),
    CAGE("Rolling cage"),
    BULK("Bulk container"),
    VIRTUAL("Virtual license plate (no physical container)");

    private final String description;

    LicensePlateType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this type requires physical container
     */
    public boolean requiresPhysicalContainer() {
        return this != VIRTUAL;
    }

    /**
     * Check if this type is typically used for picking
     */
    public boolean isPickingContainer() {
        return this == TOTE || this == CARTON;
    }

    /**
     * Check if this type is typically used for storage
     */
    public boolean isStorageContainer() {
        return this == PALLET || this == CAGE || this == BULK;
    }
}
