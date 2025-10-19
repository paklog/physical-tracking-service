package com.paklog.wes.tracking.domain.aggregate;

import com.paklog.wes.tracking.domain.valueobject.OccupancyStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LocationStateTest {

    @Test
    void create_shouldInitializeWithDefaults() {
        LocationState state = LocationState.create(
            "LOC-1", "WH-1", "ZONE-A", 10,
            new BigDecimal("100.0"), new BigDecimal("50.0")
        );

        assertEquals("LOC-1", state.getLocationId());
        assertEquals("WH-1", state.getWarehouseId());
        assertEquals("ZONE-A", state.getZone());
        assertEquals(OccupancyStatus.EMPTY, state.getOccupancyStatus());
        assertTrue(state.getLicensePlateIds().isEmpty());
        assertEquals(0, state.getCurrentQuantity());
        assertEquals(BigDecimal.ZERO, state.getCurrentWeight());
        assertEquals(BigDecimal.ZERO, state.getCurrentVolume());
        assertFalse(state.getIsBlocked());
        assertFalse(state.requiresAttention());
    }

    @Test
    void addLicensePlate_shouldUpdateQuantitiesAndStatus() {
        LocationState state = LocationState.create(
            "LOC-2", "WH-1", "ZONE-A", 20,
            new BigDecimal("200.0"), new BigDecimal("100.0")
        );

        state.addLicensePlate("LP-1", 5, new BigDecimal("25"), new BigDecimal("10"));

        assertEquals(1, state.getLicensePlateCount());
        assertEquals(5, state.getCurrentQuantity());
        assertEquals(new BigDecimal("25"), state.getCurrentWeight());
        assertEquals(new BigDecimal("10"), state.getCurrentVolume());
        assertEquals(OccupancyStatus.PARTIALLY_OCCUPIED, state.getOccupancyStatus());
        assertTrue(state.canAccept(5, new BigDecimal("25"), new BigDecimal("10")));
        assertTrue(state.getUtilizationPercentage().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void removeLicensePlate_shouldReturnToEmpty() {
        LocationState state = LocationState.create(
            "LOC-3", "WH-1", "ZONE-A", 10,
            new BigDecimal("100"), new BigDecimal("50")
        );
        state.addLicensePlate("LP-1", 5, new BigDecimal("25"), new BigDecimal("10"));

        state.removeLicensePlate("LP-1", 5, new BigDecimal("25"), new BigDecimal("10"));

        assertTrue(state.isEmpty());
        assertEquals(0, state.getLicensePlateCount());
        assertEquals(OccupancyStatus.EMPTY, state.getOccupancyStatus());
    }

    @Test
    void addLicensePlate_shouldRejectWhenOverCapacity() {
        LocationState state = LocationState.create(
            "LOC-4", "WH-1", "ZONE-B", 5,
            new BigDecimal("50"), new BigDecimal("20")
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> state.addLicensePlate("LP-1", 6, new BigDecimal("60"), new BigDecimal("30"))
        );

        assertTrue(exception.getMessage().contains("does not have sufficient capacity"));
    }

    @Test
    void addLicensePlate_shouldFailWhenBlocked() {
        LocationState state = LocationState.create(
            "LOC-5", "WH-1", "ZONE-B", 5,
            new BigDecimal("50"), new BigDecimal("20")
        );
        state.block("maintenance");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> state.addLicensePlate("LP-2", 1, BigDecimal.ONE, BigDecimal.ONE)
        );

        assertTrue(exception.getMessage().contains("is blocked"));
        assertEquals(OccupancyStatus.BLOCKED, state.getOccupancyStatus());
    }

    @Test
    void removeLicensePlate_shouldThrowWhenNotPresent() {
        LocationState state = LocationState.create(
            "LOC-6", "WH-1", "ZONE-C", 5,
            new BigDecimal("50"), new BigDecimal("20")
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> state.removeLicensePlate("LP-1", 1, BigDecimal.ONE, BigDecimal.ONE)
        );

        assertTrue(exception.getMessage().contains("not at location"));
    }

    @Test
    void unblock_shouldRestoreOccupancyStatus() {
        LocationState state = LocationState.create(
            "LOC-7", "WH-1", "ZONE-C", 5,
            new BigDecimal("50"), new BigDecimal("20")
        );
        state.addLicensePlate("LP-1", 2, new BigDecimal("10"), new BigDecimal("5"));
        state.block("maintenance");

        state.unblock();

        assertEquals(OccupancyStatus.PARTIALLY_OCCUPIED, state.getOccupancyStatus());
        assertFalse(state.getIsBlocked());
        assertNull(state.getBlockReason());
    }

    @Test
    void updateCapacityAndCoordinates_shouldRefreshState() {
        LocationState state = LocationState.create(
            "LOC-8", "WH-2", "ZONE-D", 10,
            new BigDecimal("100"), new BigDecimal("40")
        );

        state.setCoordinates(1.2, 3.4, 5.6, "RFID-1");
        state.updateCapacity(8, new BigDecimal("80"), new BigDecimal("30"));

        assertEquals(Integer.valueOf(8), state.getMaxQuantity());
        assertEquals(new BigDecimal("80"), state.getMaxWeight());
        assertEquals("RFID-1", state.getRfidZone());
    }

    @Test
    void canAccept_shouldConsiderWeightAndVolume() {
        LocationState state = LocationState.create(
            "LOC-9", "WH-2", "ZONE-E", 5,
            new BigDecimal("50"), new BigDecimal("20")
        );

        assertTrue(state.canAccept(5, new BigDecimal("50"), new BigDecimal("20")));
        assertFalse(state.canAccept(6, new BigDecimal("50"), new BigDecimal("20")));
        assertFalse(state.canAccept(1, new BigDecimal("60"), new BigDecimal("20")));
        assertFalse(state.canAccept(1, new BigDecimal("10"), new BigDecimal("25")));
    }

    @Test
    void availabilityMetricsShouldBeExposed() {
        LocationState state = LocationState.create(
            "LOC-10", "WH-2", "ZONE-F", 10,
            new BigDecimal("100"), new BigDecimal("40")
        );
        state.addLicensePlate("LP-1", 4, new BigDecimal("20"), new BigDecimal("8"));

        assertEquals(Integer.valueOf(6), state.getAvailableQuantity());
        assertTrue(state.getDwellTimeSeconds() >= 0);
    }

    @Test
    void removeLicensePlate_shouldClampNegativeTotals() {
        LocationState state = LocationState.create(
            "LOC-11", "WH-2", "ZONE-G", 10,
            new BigDecimal("100"), new BigDecimal("40")
        );
        state.addLicensePlate("LP-1", 2, new BigDecimal("10"), new BigDecimal("5"));

        state.removeLicensePlate("LP-1", 2, new BigDecimal("20"), new BigDecimal("10"));

        assertEquals(BigDecimal.ZERO, state.getCurrentWeight());
        assertEquals(BigDecimal.ZERO, state.getCurrentVolume());
        assertEquals(0, state.getCurrentQuantity());
    }
}
