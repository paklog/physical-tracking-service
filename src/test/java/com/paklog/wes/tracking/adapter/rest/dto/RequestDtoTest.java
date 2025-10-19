package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestDtoTest {

    @Test
    void recordsShouldExposeFields() {
        CreateLicensePlateRequest createRequest = new CreateLicensePlateRequest(
            "LP-REQ", "WH-REQ", LicensePlateType.TOTE, "CONT-REQ"
        );
        MoveLicensePlateRequest moveRequest = new MoveLicensePlateRequest(
            "LOC-DEST", MovementType.RELOCATION, "rebalance"
        );
        AddItemRequest addItemRequest = new AddItemRequest(
            "SKU-REQ", "LOT-REQ", 2, new BigDecimal("1.2"), new BigDecimal("0.3"), "EA"
        );

        assertEquals("LP-REQ", createRequest.licensePlateId());
        assertEquals(LicensePlateType.TOTE, createRequest.type());
        assertEquals("LOC-DEST", moveRequest.toLocationId());
        assertEquals(MovementType.RELOCATION, moveRequest.movementType());
        assertEquals("SKU-REQ", addItemRequest.sku());
        assertEquals(2, addItemRequest.quantity());
        assertEquals("EA", addItemRequest.uom());
    }
}
