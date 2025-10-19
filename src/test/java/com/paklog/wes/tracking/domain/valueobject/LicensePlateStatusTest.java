package com.paklog.wes.tracking.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LicensePlateStatusTest {

    @Test
    void movementAndItemCapabilitiesShouldMatchLifecycle() {
        assertTrue(LicensePlateStatus.ACTIVE.canBeMoved());
        assertTrue(LicensePlateStatus.AT_LOCATION.canBeMoved());
        assertFalse(LicensePlateStatus.CREATED.canBeMoved());
        assertFalse(LicensePlateStatus.CLOSED.canBeMoved());

        assertTrue(LicensePlateStatus.CREATED.canAddItems());
        assertFalse(LicensePlateStatus.SHIPPED.canAddItems());

        assertTrue(LicensePlateStatus.PICKED.canRemoveItems());
        assertFalse(LicensePlateStatus.CLOSED.canRemoveItems());
    }

    @Test
    void finalStatesShouldBeRecognised() {
        assertTrue(LicensePlateStatus.SHIPPED.isFinalState());
        assertTrue(LicensePlateStatus.CONSUMED.isFinalState());
        assertFalse(LicensePlateStatus.ACTIVE.isFinalState());
    }
}
