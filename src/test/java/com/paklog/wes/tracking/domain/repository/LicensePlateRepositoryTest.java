package com.paklog.wes.tracking.domain.repository;

import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateStatus;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataMongoTest
class LicensePlateRepositoryTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getConnectionString);
    }

    @Autowired
    private LicensePlateRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldFindActiveLicensePlatesAtLocation() {
        LicensePlate active = LicensePlate.create(
            "LP-ACTIVE", "WH-1", LicensePlateType.PALLET, "CONT-1", "tester"
        );
        active.addItem("SKU-1", "LOT-1", 5, new BigDecimal("10"), new BigDecimal("2"), "EA");
        active.moveTo("LOC-A", MovementType.PUTAWAY, "worker", "putaway");

        LicensePlate shipped = LicensePlate.create(
            "LP-SHIPPED", "WH-1", LicensePlateType.PALLET, "CONT-2", "tester"
        );
        shipped.addItem("SKU-1", "LOT-2", 5, new BigDecimal("10"), new BigDecimal("2"), "EA");
        shipped.moveTo("LOC-A", MovementType.PUTAWAY, "worker", "putaway");
        shipped.pack();
        shipped.ship();

        repository.saveAll(List.of(active, shipped));

        List<LicensePlate> result = repository.findActiveLicensePlatesAtLocation("LOC-A");

        assertEquals(1, result.size());
        assertEquals("LP-ACTIVE", result.get(0).getLicensePlateId());
        assertEquals(LicensePlateStatus.AT_LOCATION, result.get(0).getStatus());
    }

    @Test
    void shouldFindByWarehouseAndCreatedAfter() {
        LicensePlate plate1 = LicensePlate.create(
            "LP-1", "WH-1", LicensePlateType.TOTE, "CONT-3", "tester"
        );
        plate1.addItem("SKU-2", "LOT-3", 2, new BigDecimal("5"), new BigDecimal("1"), "EA");
        plate1.moveTo("LOC-B", MovementType.REPLENISHMENT, "worker", "move");

        LicensePlate plate2 = LicensePlate.create(
            "LP-2", "WH-1", LicensePlateType.TOTE, "CONT-4", "tester"
        );
        plate2.addItem("SKU-3", null, 3, new BigDecimal("3"), new BigDecimal("1"), "EA");
        plate2.moveTo(null, MovementType.RELOCATION, "worker", "in transit");

        repository.saveAll(List.of(plate1, plate2));

        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);

        List<LicensePlate> createdAfter = repository.findByWarehouseIdAndCreatedAtAfter("WH-1", lastHour);
        List<LicensePlate> inTransit = repository.findByStatus(LicensePlateStatus.IN_TRANSIT);
        List<LicensePlate> containingSku = repository.findContainingSku("SKU-2");
        List<LicensePlate> emptyAvailable = repository.findEmptyAvailableLicensePlates("WH-1");

        assertEquals(2, createdAfter.size());
        assertEquals(1, inTransit.size());
        assertEquals("LP-2", inTransit.get(0).getLicensePlateId());
        assertEquals(1, containingSku.size());
        assertEquals("LP-1", containingSku.get(0).getLicensePlateId());
        assertEquals(0, emptyAvailable.size());
    }

    @Test
    void shouldFindEmptyLicensePlates() {
        LicensePlate empty = LicensePlate.create(
            "LP-EMPTY", "WH-2", LicensePlateType.CARTON, "CONT-5", "tester"
        );
        LicensePlate consumed = LicensePlate.create(
            "LP-CONSUMED", "WH-2", LicensePlateType.CARTON, "CONT-6", "tester"
        );
        consumed.addItem("SKU-9", null, 1, BigDecimal.ONE, BigDecimal.ONE, "EA");
        consumed.removeItem("SKU-9", null, 1);

        repository.save(empty);
        repository.save(consumed);

        List<LicensePlate> emptyPlates = repository.findEmptyLicensePlates();
        List<LicensePlate> availableEmpty = repository.findEmptyAvailableLicensePlates("WH-2");

        assertEquals(2, emptyPlates.size());
        assertTrue(emptyPlates.stream().anyMatch(lp -> lp.getLicensePlateId().equals("LP-EMPTY")));
        assertTrue(emptyPlates.stream().anyMatch(lp -> lp.getLicensePlateId().equals("LP-CONSUMED")));
        assertEquals(LicensePlateStatus.CREATED, availableEmpty.get(0).getStatus());
        assertEquals(1, availableEmpty.size());

        long count = repository.countByWarehouseId("WH-2");
        assertEquals(2, count);
    }
}
