package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LicensePlateResponseTest {

    @Test
    void from_shouldMapLicensePlateFields() {
        LicensePlate plate = LicensePlate.create(
            "LP-RES", "WH-1", LicensePlateType.CARTON, "CONT-99", "tester"
        );
        plate.addItem("SKU-1", "LOT-1", 3, new BigDecimal("2.5"), new BigDecimal("1.0"), "EA");
        plate.addItem("SKU-2", null, 1, BigDecimal.ONE, BigDecimal.ONE, "EA");
        plate.moveTo("LOC-1", MovementType.PUTAWAY, "worker", "initial");

        LicensePlateResponse response = LicensePlateResponse.from(plate);

        assertEquals("LP-RES", response.licensePlateId());
        assertEquals("WH-1", response.warehouseId());
        assertEquals(LicensePlateType.CARTON, response.type());
        assertEquals("LOC-1", response.currentLocationId());
        assertEquals(4, response.totalQuantity());
        assertEquals(2, response.items().size());
        assertEquals("SKU-1", response.items().get(0).sku());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }
}
