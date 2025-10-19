package com.paklog.wes.tracking.domain.aggregate;

import com.paklog.wes.tracking.domain.entity.Movement;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateStatus;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LicensePlateTest {

    @Test
    void createLicensePlate_initialState() {
        LicensePlate plate = LicensePlate.create(
            "LP-001", "WH-001", LicensePlateType.PALLET, "CONT-123", "tester"
        );

        assertEquals("LP-001", plate.getLicensePlateId());
        assertEquals(LicensePlateStatus.CREATED, plate.getStatus());
        assertEquals("WH-001", plate.getWarehouseId());
        assertEquals(0, plate.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, plate.getTotalWeight());
        assertEquals(BigDecimal.ZERO, plate.getTotalVolume());
        assertTrue(plate.getItems().isEmpty());
        assertNotNull(plate.getCreatedAt());
        assertNotNull(plate.getUpdatedAt());
    }

    @Test
    void addItem_shouldAccumulateAndActivate() {
        LicensePlate plate = LicensePlate.create(
            "LP-002", "WH-001", LicensePlateType.CARTON, "CONT-321", "tester"
        );

        plate.addItem("SKU-1", "LOT-1", 5, new BigDecimal("4.5"), new BigDecimal("1.2"), "EA");
        plate.addItem("SKU-1", "LOT-1", 3, new BigDecimal("4.5"), new BigDecimal("1.2"), "EA");
        plate.addItem("SKU-2", null, 2, null, null, "EA");

        assertEquals(LicensePlateStatus.ACTIVE, plate.getStatus());
        assertEquals(10, plate.getTotalQuantity());
        assertEquals(new BigDecimal("4.5"), plate.getTotalWeight());
        assertEquals(new BigDecimal("1.2"), plate.getTotalVolume());
        assertFalse(plate.isEmpty());
        assertTrue(plate.containsSku("SKU-2"));
        assertEquals(2, plate.getItems().size());
    }

    @Test
    void removeItem_shouldConsumeWhenEmpty() {
        LicensePlate plate = LicensePlate.create(
            "LP-003", "WH-001", LicensePlateType.TOTE, "CONT-213", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 5, new BigDecimal("2.0"), new BigDecimal("0.5"), "EA");

        plate.removeItem("SKU-1", "LOT-1", 5);

        assertTrue(plate.isEmpty());
        assertEquals(LicensePlateStatus.CONSUMED, plate.getStatus());
        assertEquals(0, plate.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, plate.getTotalWeight());
        assertEquals(BigDecimal.ZERO, plate.getTotalVolume());
    }

    @Test
    void moveTo_shouldRecordMovementAndUpdateStatus() {
        LicensePlate plate = LicensePlate.create(
            "LP-004", "WH-001", LicensePlateType.PALLET, "CONT-555", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 4, new BigDecimal("1.0"), new BigDecimal("0.4"), "EA");

        plate.moveTo("LOC-100", MovementType.PUTAWAY, "worker-1", "initial putaway");
        Movement movement = plate.getLatestMovement().orElseThrow();

        assertEquals("LOC-100", plate.getCurrentLocationId());
        assertEquals(LicensePlateStatus.AT_LOCATION, plate.getStatus());
        assertEquals(MovementType.PUTAWAY, movement.getType());
        assertNull(movement.getFromLocationId());
        assertEquals("LOC-100", movement.getToLocationId());
        assertEquals("worker-1", movement.getPerformedBy());
        assertEquals("initial putaway", movement.getReason());

        plate.moveTo(null, MovementType.RELOCATION, "worker-2", "staging");

        assertNull(plate.getCurrentLocationId());
        assertEquals(LicensePlateStatus.IN_TRANSIT, plate.getStatus());
        assertEquals(2, plate.getMovements().size());
    }

    @Test
    void addItem_shouldThrowWhenStatusDoesNotAllow() {
        LicensePlate plate = LicensePlate.create(
            "LP-005", "WH-001", LicensePlateType.PALLET, "CONT-789", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 1, new BigDecimal("0.5"), new BigDecimal("0.1"), "EA");
        plate.pack();
        plate.ship();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> plate.addItem("SKU-2", null, 1, BigDecimal.ONE, BigDecimal.ONE, "EA")
        );

        assertTrue(exception.getMessage().contains("Cannot add items"));
    }

    @Test
    void removeItem_shouldThrowWhenSkuNotPresent() {
        LicensePlate plate = LicensePlate.create(
            "LP-006", "WH-001", LicensePlateType.PALLET, "CONT-999", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 2, new BigDecimal("0.5"), new BigDecimal("0.1"), "EA");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> plate.removeItem("SKU-2", "LOT-2", 1)
        );

        assertTrue(exception.getMessage().contains("Item not found"));
    }

    @Test
    void pickingLifecycle_shouldTransitionStatuses() {
        LicensePlate plate = LicensePlate.create(
            "LP-007", "WH-001", LicensePlateType.PALLET, "CONT-777", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 3, BigDecimal.ONE, BigDecimal.ONE, "EA");
        plate.moveTo("LOC-1", MovementType.PUTAWAY, "worker", "putaway");

        plate.startPicking();
        assertEquals(LicensePlateStatus.PICKED, plate.getStatus());

        plate.completePicking();
        assertEquals(LicensePlateStatus.AT_LOCATION, plate.getStatus());

        plate.removeItem("SKU-1", "LOT-1", 3);
        assertEquals(LicensePlateStatus.CONSUMED, plate.getStatus());
        assertThrows(IllegalStateException.class, plate::startPicking);
    }

    @Test
    void packShipAndClose_shouldUpdateStatus() {
        LicensePlate plate = LicensePlate.create(
            "LP-008", "WH-001", LicensePlateType.CARTON, "CONT-888", "tester"
        );
        plate.addItem("SKU-1", null, 1, BigDecimal.ONE, BigDecimal.ONE, "EA");
        plate.moveTo("LOC-2", MovementType.PUTAWAY, "worker", "putaway");

        plate.pack();
        assertEquals(LicensePlateStatus.PACKED, plate.getStatus());

        plate.ship();
        assertEquals(LicensePlateStatus.SHIPPED, plate.getStatus());

        plate.close();
        assertEquals(LicensePlateStatus.CLOSED, plate.getStatus());
    }

    @Test
    void ownerAndOrderQueriesShouldWork() {
        LicensePlate plate = LicensePlate.create(
            "LP-009", "WH-001", LicensePlateType.TOTE, "CONT-999", "tester"
        );
        plate.addItem("SKU-ORDER", null, 2, BigDecimal.ONE, BigDecimal.ONE, "EA");
        plate.getItem("SKU-ORDER", null).ifPresent(item -> item.associateWithOrder("ORDER-123"));
        plate.setOwner("OWNER-1");
        plate.moveTo("LOC-3", MovementType.PUTAWAY, "worker", "putaway");

        assertEquals("OWNER-1", plate.getOwnerId());
        assertEquals(1, plate.getItemsForOrder("ORDER-123").size());
        assertTrue(plate.getLatestMovement().isPresent());
        assertEquals(1, plate.getMovementsToLocation("LOC-3").size());
    }

    @Test
    void moveTo_shouldThrowWhenStatusPreventsMovement() {
        LicensePlate plate = LicensePlate.create(
            "LP-010", "WH-002", LicensePlateType.CARTON, "CONT-010", "tester"
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> plate.moveTo("LOC-1", MovementType.PUTAWAY, "worker", "reason")
        );

        assertTrue(exception.getMessage().contains("Cannot move"));
    }
}
