package com.paklog.wes.tracking.domain.valueobject;

/**
 * Movement Type - Type of physical movement
 */
public enum MovementType {
    PUTAWAY("Receiving putaway"),
    PICK("Pick from location"),
    REPLENISHMENT("Stock replenishment"),
    RELOCATION("Internal relocation/move"),
    CYCLE_COUNT("Cycle count adjustment"),
    CONSOLIDATION("Consolidate license plates"),
    SHIP("Ship from dock"),
    RETURN("Return to stock");

    private final String description;

    MovementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if movement adds inventory to location
     */
    public boolean addsInventory() {
        return switch (this) {
            case PUTAWAY, REPLENISHMENT, RELOCATION, RETURN -> true;
            case PICK, CONSOLIDATION, SHIP, CYCLE_COUNT -> false;
        };
    }

    /**
     * Check if movement removes inventory from location
     */
    public boolean removesInventory() {
        return switch (this) {
            case PICK, CONSOLIDATION, SHIP, RELOCATION -> true;
            case PUTAWAY, REPLENISHMENT, RETURN, CYCLE_COUNT -> false;
        };
    }
}
