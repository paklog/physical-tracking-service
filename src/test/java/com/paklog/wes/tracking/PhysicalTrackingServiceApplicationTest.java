package com.paklog.wes.tracking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PhysicalTrackingServiceApplicationTest {

    @Test
    void mainShouldStartApplicationContext() {
        assertDoesNotThrow(() -> PhysicalTrackingServiceApplication.main(new String[]{"--spring.main.web-application-type=none"}));
    }
}
