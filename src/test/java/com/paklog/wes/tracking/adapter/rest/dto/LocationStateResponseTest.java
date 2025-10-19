package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.valueobject.OccupancyStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LocationStateResponseTest {

    @Test
    void from_shouldMapLocationStateFields() {
        LocationState state = LocationState.create(
            "LOC-10", "WH-2", "ZONE-B", 20,
            new BigDecimal("200"), new BigDecimal("100")
        );
        state.addLicensePlate("LP-1", 5, new BigDecimal("10"), new BigDecimal("4"));
        state.block("maintenance");

        LocationStateResponse response = LocationStateResponse.from(state);

        assertEquals("LOC-10", response.locationId());
        assertEquals("WH-2", response.warehouseId());
        assertEquals(OccupancyStatus.BLOCKED, response.occupancyStatus());
        assertEquals(1, response.licensePlateIds().size());
        assertEquals(new BigDecimal("200"), response.maxWeight());
        assertTrue(response.isBlocked());
        assertEquals("maintenance", response.blockReason());
        assertNotNull(response.lastUpdated());
    }
}
