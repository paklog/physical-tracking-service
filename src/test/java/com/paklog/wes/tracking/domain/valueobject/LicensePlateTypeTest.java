package com.paklog.wes.tracking.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LicensePlateTypeTest {

    @Test
    void descriptionsShouldBeAvailableForAllTypes() {
        for (LicensePlateType type : LicensePlateType.values()) {
            assertNotNull(type.getDescription());
        }
    }
}
