package com.paklog.wes.tracking.domain.repository;

import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateStatus;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for LicensePlate aggregate
 */
@Repository
public interface LicensePlateRepository extends MongoRepository<LicensePlate, String> {

    /**
     * Find license plates by warehouse
     */
    List<LicensePlate> findByWarehouseId(String warehouseId);

    /**
     * Find license plates by warehouse and status
     */
    List<LicensePlate> findByWarehouseIdAndStatus(String warehouseId, LicensePlateStatus status);

    /**
     * Find license plates by warehouse and type
     */
    List<LicensePlate> findByWarehouseIdAndType(String warehouseId, LicensePlateType type);

    /**
     * Find license plates at a location
     */
    List<LicensePlate> findByCurrentLocationId(String locationId);

    /**
     * Find license plates at location with specific status
     */
    List<LicensePlate> findByCurrentLocationIdAndStatus(String locationId, LicensePlateStatus status);

    /**
     * Find by container code (barcode/RFID)
     */
    Optional<LicensePlate> findByContainerCode(String containerCode);

    /**
     * Find by owner
     */
    List<LicensePlate> findByOwnerId(String ownerId);

    /**
     * Find active license plates
     */
    @Query("{'status': {$in: ['ACTIVE', 'AT_LOCATION', 'IN_TRANSIT']}}")
    List<LicensePlate> findActiveLicensePlates();

    /**
     * Find active license plates at location
     */
    @Query("{'currentLocationId': ?0, 'status': {$in: ['ACTIVE', 'AT_LOCATION']}}")
    List<LicensePlate> findActiveLicensePlatesAtLocation(String locationId);

    /**
     * Find license plates created after timestamp
     */
    List<LicensePlate> findByCreatedAtAfter(LocalDateTime timestamp);

    /**
     * Find license plates by warehouse created after timestamp
     */
    List<LicensePlate> findByWarehouseIdAndCreatedAtAfter(String warehouseId, LocalDateTime timestamp);

    /**
     * Find in-transit license plates
     */
    List<LicensePlate> findByStatus(LicensePlateStatus status);

    /**
     * Find license plates containing specific SKU
     */
    @Query("{'items.sku': ?0}")
    List<LicensePlate> findContainingSku(String sku);

    /**
     * Find license plates containing specific SKU at location
     */
    @Query("{'currentLocationId': ?0, 'items.sku': ?1}")
    List<LicensePlate> findContainingSkuAtLocation(String locationId, String sku);

    /**
     * Find empty license plates
     */
    @Query("{'items': {$size: 0}}")
    List<LicensePlate> findEmptyLicensePlates();

    /**
     * Find license plates by warehouse and empty status
     */
    @Query("{'warehouseId': ?0, 'items': {$size: 0}, 'status': {$nin: ['CONSUMED', 'CLOSED']}}")
    List<LicensePlate> findEmptyAvailableLicensePlates(String warehouseId);

    /**
     * Count license plates by warehouse
     */
    long countByWarehouseId(String warehouseId);

    /**
     * Count license plates at location
     */
    long countByCurrentLocationId(String locationId);

    /**
     * Count license plates by status
     */
    long countByWarehouseIdAndStatus(String warehouseId, LicensePlateStatus status);
}
