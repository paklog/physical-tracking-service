package com.paklog.wes.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Physical Tracking Service - WES
 * Handles license plate management, movements, and real-time location state
 */
@SpringBootApplication
public class PhysicalTrackingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhysicalTrackingServiceApplication.class, args);
    }
}
