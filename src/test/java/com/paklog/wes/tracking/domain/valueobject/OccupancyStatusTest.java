package com.paklog.wes.tracking.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OccupancyStatusTest {

    @Test
    void fromUtilization_shouldMapRanges() {
        assertEquals(OccupancyStatus.UNKNOWN, OccupancyStatus.fromUtilization(-1));
        assertEquals(OccupancyStatus.EMPTY, OccupancyStatus.fromUtilization(0));
        assertEquals(OccupancyStatus.PARTIALLY_OCCUPIED, OccupancyStatus.fromUtilization(50));
        assertEquals(OccupancyStatus.FULL, OccupancyStatus.fromUtilization(99));
        assertEquals(OccupancyStatus.OVER_CAPACITY, OccupancyStatus.fromUtilization(150));
    }

    @Test
    void statusFlagsShouldReflectCapabilities() {
        assertTrue(OccupancyStatus.EMPTY.canAcceptInventory());
        assertTrue(OccupancyStatus.PARTIALLY_OCCUPIED.canAcceptInventory());
        assertFalse(OccupancyStatus.FULL.canAcceptInventory());

        assertTrue(OccupancyStatus.BLOCKED.requiresAttention());
        assertTrue(OccupancyStatus.OVER_CAPACITY.requiresAttention());
        assertFalse(OccupancyStatus.EMPTY.requiresAttention());
    }
}
