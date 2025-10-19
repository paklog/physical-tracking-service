package com.paklog.wes.tracking.domain.repository;

import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.valueobject.OccupancyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataMongoTest
class LocationStateRepositoryTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getConnectionString);
    }

    @Autowired
    private LocationStateRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldFindAvailableAndBlockedLocations() {
        LocationState available = LocationState.create(
            "LOC-1", "WH-1", "ZONE-A", 10, new BigDecimal("100"), new BigDecimal("50")
        );

        LocationState occupied = LocationState.create(
            "LOC-2", "WH-1", "ZONE-A", 10, new BigDecimal("100"), new BigDecimal("50")
        );
        occupied.addLicensePlate("LP-1", 9, new BigDecimal("90"), new BigDecimal("45"));

        LocationState blocked = LocationState.create(
            "LOC-3", "WH-1", "ZONE-A", 10, new BigDecimal("100"), new BigDecimal("50")
        );
        blocked.block("maintenance");

        repository.saveAll(List.of(available, occupied, blocked));

        List<LocationState> availableLocations = repository.findAvailableLocations("WH-1", "ZONE-A");
        List<LocationState> blockedLocations = repository.findByWarehouseIdAndIsBlockedTrue("WH-1");
        long blockedCount = repository.countByWarehouseIdAndIsBlockedTrue("WH-1");

        assertEquals(2, availableLocations.size());
        assertTrue(availableLocations.stream().anyMatch(state -> state.getLocationId().equals("LOC-1")));
        assertTrue(availableLocations.stream().anyMatch(state -> state.getLocationId().equals("LOC-2")));
        assertEquals(OccupancyStatus.EMPTY, availableLocations.stream()
            .filter(state -> state.getLocationId().equals("LOC-1"))
            .findFirst()
            .orElseThrow()
            .getOccupancyStatus());
        assertEquals(OccupancyStatus.PARTIALLY_OCCUPIED, availableLocations.stream()
            .filter(state -> state.getLocationId().equals("LOC-2"))
            .findFirst()
            .orElseThrow()
            .getOccupancyStatus());

        assertEquals(1, blockedLocations.size());
        assertEquals("LOC-3", blockedLocations.get(0).getLocationId());
        assertEquals(1, blockedCount);
    }

    @Test
    void shouldFindContainingLicensePlateAndFullLocations() {
        LocationState full = LocationState.create(
            "LOC-4", "WH-2", "ZONE-B", 5, new BigDecimal("50"), new BigDecimal("20")
        );
        full.addLicensePlate("LP-2", 5, new BigDecimal("50"), new BigDecimal("20"));

        LocationState attention = LocationState.create(
            "LOC-5", "WH-2", "ZONE-B", 5, new BigDecimal("50"), new BigDecimal("20")
        );
        attention.addLicensePlate("LP-3", 4, new BigDecimal("40"), new BigDecimal("18"));
        attention.updateCapacity(3, new BigDecimal("30"), new BigDecimal("15"));

        repository.save(full);
        repository.save(attention);

        List<LocationState> fullLocations = repository.findFullLocations("WH-2");
        List<LocationState> requiringAttention = repository.findLocationsRequiringAttention("WH-2");
        List<LocationState> containing = repository.findContainingLicensePlate("LP-2");

        assertEquals(2, fullLocations.size());
        assertTrue(fullLocations.stream().anyMatch(state -> state.getLocationId().equals("LOC-4")));
        assertTrue(fullLocations.stream().anyMatch(state -> state.getLocationId().equals("LOC-5")));

        assertEquals(1, requiringAttention.size());
        assertEquals("LOC-5", requiringAttention.get(0).getLocationId());

        assertEquals(1, containing.size());
        assertEquals("LOC-4", containing.get(0).getLocationId());
    }
}
