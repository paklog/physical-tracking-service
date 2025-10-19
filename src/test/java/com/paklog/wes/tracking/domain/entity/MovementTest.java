package com.paklog.wes.tracking.domain.entity;

import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovementTest {

    @Test
    void create_shouldPopulateMandatoryFields() {
        Movement movement = Movement.create(
            MovementType.PUTAWAY, null, "LOC-1", "worker-1", "initial"
        );

        assertNotNull(movement.getMovementId());
        assertEquals(MovementType.PUTAWAY, movement.getType());
        assertNull(movement.getFromLocationId());
        assertEquals("LOC-1", movement.getToLocationId());
        assertEquals("worker-1", movement.getPerformedBy());
        assertEquals("initial", movement.getReason());
        assertNotNull(movement.getOccurredAt());
        assertTrue(movement.isInbound());
        assertFalse(movement.isOutbound());
    }

    @Test
    void create_shouldRejectNullType() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Movement.create(null, "LOC-A", "LOC-B", "worker", "reason")
        );
    }

    @Test
    void inboundAndOutbound_shouldReflectMovementType() {
        Movement inbound = Movement.create(
            MovementType.REPLENISHMENT, "RESERVE", "PICK", "worker", "replenish"
        );
        Movement outbound = Movement.create(
            MovementType.PICK, "PICK", "PACK", "picker", "order"
        );

        assertTrue(inbound.isInbound());
        assertFalse(inbound.isOutbound());

        assertFalse(outbound.isInbound());
        assertTrue(outbound.isOutbound());
    }

    @Test
    void getTimeSinceMovement_shouldCalculateSeconds() {
        Movement first = Movement.create(
            MovementType.PUTAWAY, null, "LOC-1", "worker", "start"
        );
        Movement second = Movement.create(
            MovementType.RELOCATION, "LOC-1", "LOC-2", "worker", "move"
        );

        assertTrue(second.getTimeSinceMovement(first) >= 0);
        assertEquals(0, second.getTimeSinceMovement(null));
    }

    @Test
    void associationsShouldBeStored() {
        Movement movement = Movement.create(
            MovementType.PICK, "LOC-1", "LOC-2", "worker", "order"
        );
        movement.associateWithTask("TASK-1");
        movement.associateWithWave("WAVE-1");

        assertEquals("TASK-1", movement.getTaskId());
        assertEquals("WAVE-1", movement.getWaveId());
        assertNotNull(movement.toString());
    }
}
