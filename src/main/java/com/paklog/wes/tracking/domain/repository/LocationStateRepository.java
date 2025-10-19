package com.paklog.wes.tracking.domain.repository;

import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.valueobject.OccupancyStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for LocationState aggregate
 */
@Repository
public interface LocationStateRepository extends MongoRepository<LocationState, String> {

    /**
     * Find location states by warehouse
     */
    List<LocationState> findByWarehouseId(String warehouseId);

    /**
     * Find location states by warehouse and zone
     */
    List<LocationState> findByWarehouseIdAndZone(String warehouseId, String zone);

    /**
     * Find location states by warehouse and occupancy status
     */
    List<LocationState> findByWarehouseIdAndOccupancyStatus(
        String warehouseId,
        OccupancyStatus occupancyStatus
    );

    /**
     * Find blocked locations
     */
    List<LocationState> findByWarehouseIdAndIsBlockedTrue(String warehouseId);

    /**
     * Find empty locations
     */
    @Query("{'warehouseId': ?0, 'occupancyStatus': 'EMPTY'}")
    List<LocationState> findEmptyLocations(String warehouseId);

    /**
     * Find full locations
     */
    @Query("{'warehouseId': ?0, 'occupancyStatus': {$in: ['FULL', 'OVER_CAPACITY']}}")
    List<LocationState> findFullLocations(String warehouseId);

    /**
     * Find locations requiring attention
     */
    @Query("{'warehouseId': ?0, 'occupancyStatus': {$in: ['OVER_CAPACITY', 'BLOCKED']}}")
    List<LocationState> findLocationsRequiringAttention(String warehouseId);

    /**
     * Find locations with capacity available
     */
    @Query("{'warehouseId': ?0, 'zone': ?1, 'occupancyStatus': {$in: ['EMPTY', 'PARTIALLY_OCCUPIED']}, 'isBlocked': false}")
    List<LocationState> findAvailableLocations(String warehouseId, String zone);

    /**
     * Find locations containing specific license plate
     */
    @Query("{'licensePlateIds': ?0}")
    List<LocationState> findContainingLicensePlate(String licensePlateId);

    /**
     * Find locations with no movement since timestamp
     */
    @Query("{'warehouseId': ?0, 'lastMovementAt': {$lt: ?1}}")
    List<LocationState> findStaleLocations(String warehouseId, LocalDateTime since);

    /**
     * Find locations in RFID zone
     */
    List<LocationState> findByWarehouseIdAndRfidZone(String warehouseId, String rfidZone);

    /**
     * Find locations updated after timestamp
     */
    List<LocationState> findByWarehouseIdAndLastUpdatedAfter(
        String warehouseId,
        LocalDateTime timestamp
    );

    /**
     * Count locations by occupancy status
     */
    long countByWarehouseIdAndOccupancyStatus(String warehouseId, OccupancyStatus status);

    /**
     * Count blocked locations
     */
    long countByWarehouseIdAndIsBlockedTrue(String warehouseId);
}
