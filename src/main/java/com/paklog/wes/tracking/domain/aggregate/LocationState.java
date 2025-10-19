package com.paklog.wes.tracking.domain.aggregate;

import com.paklog.wes.tracking.domain.valueobject.OccupancyStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * LocationState - Aggregate root for real-time location state
 *
 * Tracks the physical state of a location including occupancy,
 * license plates present, and capacity utilization.
 */
@Document(collection = "location_states")
public class LocationState {

    @Id
    private String locationId;

    @Indexed
    private String warehouseId;

    @Indexed
    private String zone;

    private OccupancyStatus occupancyStatus;

    // License plates at this location
    private List<String> licensePlateIds;

    // Capacity tracking
    private Integer maxQuantity;
    private Integer currentQuantity;
    private BigDecimal maxWeight;
    private BigDecimal currentWeight;
    private BigDecimal maxVolume;
    private BigDecimal currentVolume;

    // State
    private Boolean isBlocked;
    private String blockReason;
    private LocalDateTime blockedAt;

    // Timestamps
    @Indexed
    private LocalDateTime lastUpdated;
    private LocalDateTime lastMovementAt;

    // RTLS integration
    private Double xCoordinate;
    private Double yCoordinate;
    private Double zCoordinate;
    private String rfidZone; // RTLS zone identifier

    protected LocationState() {
        // MongoDB constructor
    }

    /**
     * Create new location state
     */
    public static LocationState create(
            String locationId,
            String warehouseId,
            String zone,
            Integer maxQuantity,
            BigDecimal maxWeight,
            BigDecimal maxVolume
    ) {
        LocationState state = new LocationState();
        state.locationId = locationId;
        state.warehouseId = warehouseId;
        state.zone = zone;
        state.occupancyStatus = OccupancyStatus.EMPTY;
        state.licensePlateIds = new ArrayList<>();
        state.maxQuantity = maxQuantity;
        state.currentQuantity = 0;
        state.maxWeight = maxWeight;
        state.currentWeight = BigDecimal.ZERO;
        state.maxVolume = maxVolume;
        state.currentVolume = BigDecimal.ZERO;
        state.isBlocked = false;
        state.lastUpdated = LocalDateTime.now();

        return state;
    }

    /**
     * Add license plate to location
     */
    public void addLicensePlate(String licensePlateId, Integer quantity,
                               BigDecimal weight, BigDecimal volume) {
        if (isBlocked) {
            throw new IllegalStateException(
                String.format("Location %s is blocked: %s", locationId, blockReason));
        }

        // Check capacity
        if (!canAccept(quantity, weight, volume)) {
            throw new IllegalStateException(
                String.format("Location %s does not have sufficient capacity", locationId));
        }

        // Add license plate
        if (!licensePlateIds.contains(licensePlateId)) {
            licensePlateIds.add(licensePlateId);
        }

        // Update quantities
        this.currentQuantity += quantity;
        this.currentWeight = this.currentWeight.add(weight != null ? weight : BigDecimal.ZERO);
        this.currentVolume = this.currentVolume.add(volume != null ? volume : BigDecimal.ZERO);

        // Update occupancy status
        updateOccupancyStatus();

        this.lastMovementAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Remove license plate from location
     */
    public void removeLicensePlate(String licensePlateId, Integer quantity,
                                   BigDecimal weight, BigDecimal volume) {
        if (!licensePlateIds.contains(licensePlateId)) {
            throw new IllegalArgumentException(
                String.format("License plate %s not at location %s", licensePlateId, locationId));
        }

        // Remove license plate
        licensePlateIds.remove(licensePlateId);

        // Update quantities
        this.currentQuantity = Math.max(0, this.currentQuantity - quantity);
        this.currentWeight = this.currentWeight.subtract(weight != null ? weight : BigDecimal.ZERO);
        this.currentVolume = this.currentVolume.subtract(volume != null ? volume : BigDecimal.ZERO);

        // Ensure no negative values
        if (this.currentWeight.compareTo(BigDecimal.ZERO) < 0) {
            this.currentWeight = BigDecimal.ZERO;
        }
        if (this.currentVolume.compareTo(BigDecimal.ZERO) < 0) {
            this.currentVolume = BigDecimal.ZERO;
        }

        // Update occupancy status
        updateOccupancyStatus();

        this.lastMovementAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Block location
     */
    public void block(String reason) {
        this.isBlocked = true;
        this.blockReason = reason;
        this.blockedAt = LocalDateTime.now();
        this.occupancyStatus = OccupancyStatus.BLOCKED;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Unblock location
     */
    public void unblock() {
        this.isBlocked = false;
        this.blockReason = null;
        this.blockedAt = null;
        updateOccupancyStatus();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Update capacity limits
     */
    public void updateCapacity(Integer maxQuantity, BigDecimal maxWeight, BigDecimal maxVolume) {
        this.maxQuantity = maxQuantity;
        this.maxWeight = maxWeight;
        this.maxVolume = maxVolume;
        updateOccupancyStatus();
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Set RTLS coordinates
     */
    public void setCoordinates(Double x, Double y, Double z, String rfidZone) {
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.zCoordinate = z;
        this.rfidZone = rfidZone;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Check if can accept additional inventory
     */
    public boolean canAccept(Integer quantity, BigDecimal weight, BigDecimal volume) {
        if (isBlocked) {
            return false;
        }

        if (maxQuantity != null && currentQuantity + quantity > maxQuantity) {
            return false;
        }

        if (maxWeight != null && weight != null &&
            currentWeight.add(weight).compareTo(maxWeight) > 0) {
            return false;
        }

        if (maxVolume != null && volume != null &&
            currentVolume.add(volume).compareTo(maxVolume) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Calculate utilization percentage
     */
    public BigDecimal getUtilizationPercentage() {
        if (maxQuantity == null || maxQuantity == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantityPct = new BigDecimal(currentQuantity)
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(maxQuantity), 2, RoundingMode.HALF_UP);

        BigDecimal weightPct = BigDecimal.ZERO;
        if (maxWeight != null && maxWeight.compareTo(BigDecimal.ZERO) > 0) {
            weightPct = currentWeight
                .multiply(new BigDecimal("100"))
                .divide(maxWeight, 2, RoundingMode.HALF_UP);
        }

        BigDecimal volumePct = BigDecimal.ZERO;
        if (maxVolume != null && maxVolume.compareTo(BigDecimal.ZERO) > 0) {
            volumePct = currentVolume
                .multiply(new BigDecimal("100"))
                .divide(maxVolume, 2, RoundingMode.HALF_UP);
        }

        // Return maximum of the three percentages
        return quantityPct.max(weightPct).max(volumePct);
    }

    /**
     * Get available capacity
     */
    public Integer getAvailableQuantity() {
        if (maxQuantity == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, maxQuantity - currentQuantity);
    }

    /**
     * Get number of license plates
     */
    public int getLicensePlateCount() {
        return licensePlateIds.size();
    }

    /**
     * Check if location is empty
     */
    public boolean isEmpty() {
        return licensePlateIds.isEmpty() && currentQuantity == 0;
    }

    /**
     * Check if location is full
     */
    public boolean isFull() {
        return occupancyStatus == OccupancyStatus.FULL ||
               occupancyStatus == OccupancyStatus.OVER_CAPACITY;
    }

    /**
     * Check if location requires attention
     */
    public boolean requiresAttention() {
        return occupancyStatus.requiresAttention();
    }

    /**
     * Get dwell time (time since last movement)
     */
    public long getDwellTimeSeconds() {
        if (lastMovementAt == null) {
            return 0;
        }
        return java.time.Duration.between(lastMovementAt, LocalDateTime.now()).getSeconds();
    }

    /**
     * Update occupancy status based on current state
     */
    private void updateOccupancyStatus() {
        if (isBlocked) {
            this.occupancyStatus = OccupancyStatus.BLOCKED;
            return;
        }

        if (isEmpty()) {
            this.occupancyStatus = OccupancyStatus.EMPTY;
            return;
        }

        BigDecimal utilizationPct = getUtilizationPercentage();
        this.occupancyStatus = OccupancyStatus.fromUtilization(utilizationPct.doubleValue());
    }

    // Getters
    public String getLocationId() {
        return locationId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getZone() {
        return zone;
    }

    public OccupancyStatus getOccupancyStatus() {
        return occupancyStatus;
    }

    public List<String> getLicensePlateIds() {
        return new ArrayList<>(licensePlateIds);
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getMaxWeight() {
        return maxWeight;
    }

    public BigDecimal getCurrentWeight() {
        return currentWeight;
    }

    public BigDecimal getMaxVolume() {
        return maxVolume;
    }

    public BigDecimal getCurrentVolume() {
        return currentVolume;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public LocalDateTime getLastMovementAt() {
        return lastMovementAt;
    }

    public Double getXCoordinate() {
        return xCoordinate;
    }

    public Double getYCoordinate() {
        return yCoordinate;
    }

    public Double getZCoordinate() {
        return zCoordinate;
    }

    public String getRfidZone() {
        return rfidZone;
    }

    @Override
    public String toString() {
        return String.format("LocationState[id=%s, status=%s, LPs=%d, util=%s%%, blocked=%s]",
            locationId, occupancyStatus, licensePlateIds.size(),
            getUtilizationPercentage(), isBlocked);
    }
}
