package com.paklog.wes.tracking.domain.aggregate;

import com.paklog.wes.tracking.domain.entity.LPItem;
import com.paklog.wes.tracking.domain.entity.Movement;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateStatus;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import com.paklog.wes.tracking.domain.valueobject.MovementType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LicensePlate - Aggregate root for physical container/license plate
 *
 * Represents a physical container (pallet, tote, carton) that holds inventory
 * and can move through the warehouse.
 */
@Document(collection = "license_plates")
public class LicensePlate {

    @Id
    private String licensePlateId;

    @Indexed
    private String warehouseId;

    private LicensePlateType type;
    private LicensePlateStatus status;

    @Indexed
    private String currentLocationId;

    private String containerCode; // Physical barcode/RFID
    private String ownerId; // Customer/owner if applicable

    // Contents
    private List<LPItem> items;

    // Movement history
    private List<Movement> movements;

    // Totals
    private Integer totalQuantity;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    // Lifecycle
    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private String createdBy;

    protected LicensePlate() {
        // MongoDB constructor
    }

    /**
     * Create a new license plate
     */
    public static LicensePlate create(
            String licensePlateId,
            String warehouseId,
            LicensePlateType type,
            String containerCode,
            String createdBy
    ) {
        LicensePlate lp = new LicensePlate();
        lp.licensePlateId = licensePlateId;
        lp.warehouseId = warehouseId;
        lp.type = type;
        lp.status = LicensePlateStatus.CREATED;
        lp.containerCode = containerCode;
        lp.items = new ArrayList<>();
        lp.movements = new ArrayList<>();
        lp.totalQuantity = 0;
        lp.totalWeight = BigDecimal.ZERO;
        lp.totalVolume = BigDecimal.ZERO;
        lp.createdAt = LocalDateTime.now();
        lp.updatedAt = LocalDateTime.now();
        lp.createdBy = createdBy;

        return lp;
    }

    /**
     * Add item to license plate
     */
    public void addItem(String sku, String lotNumber, Integer quantity,
                       BigDecimal weight, BigDecimal volume, String uom) {
        if (!status.canAddItems()) {
            throw new IllegalStateException(
                String.format("Cannot add items to license plate in status %s", status));
        }

        // Check if item with same SKU and lot already exists
        Optional<LPItem> existing = items.stream()
            .filter(item -> item.matches(sku, lotNumber))
            .findFirst();

        if (existing.isPresent()) {
            // Add to existing item
            existing.get().addQuantity(quantity);
        } else {
            // Create new item
            LPItem newItem = LPItem.create(sku, lotNumber, quantity, weight, volume, uom);
            items.add(newItem);
        }

        // Update totals
        recalculateTotals();

        // Activate if first item
        if (status == LicensePlateStatus.CREATED) {
            this.status = LicensePlateStatus.ACTIVE;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remove item from license plate
     */
    public void removeItem(String sku, String lotNumber, Integer quantity) {
        if (!status.canRemoveItems()) {
            throw new IllegalStateException(
                String.format("Cannot remove items from license plate in status %s", status));
        }

        LPItem item = items.stream()
            .filter(i -> i.matches(sku, lotNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Item not found: SKU=%s, Lot=%s", sku, lotNumber)));

        item.removeQuantity(quantity);

        // Remove item if quantity is zero
        if (item.getQuantity() == 0) {
            items.remove(item);
        }

        // Update totals
        recalculateTotals();

        // Mark as consumed if empty
        if (items.isEmpty()) {
            this.status = LicensePlateStatus.CONSUMED;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Move license plate to a new location
     */
    public void moveTo(String newLocationId, MovementType movementType,
                      String performedBy, String reason) {
        if (!status.canBeMoved()) {
            throw new IllegalStateException(
                String.format("Cannot move license plate in status %s", status));
        }

        String previousLocation = this.currentLocationId;

        // Create movement record
        Movement movement = Movement.create(
            movementType,
            previousLocation,
            newLocationId,
            performedBy,
            reason
        );
        movements.add(movement);

        // Update current location
        this.currentLocationId = newLocationId;

        // Update status
        if (newLocationId != null) {
            this.status = LicensePlateStatus.AT_LOCATION;
        } else {
            this.status = LicensePlateStatus.IN_TRANSIT;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Start picking from license plate
     */
    public void startPicking() {
        if (status != LicensePlateStatus.AT_LOCATION && status != LicensePlateStatus.ACTIVE) {
            throw new IllegalStateException(
                String.format("Cannot start picking from license plate in status %s", status));
        }

        this.status = LicensePlateStatus.PICKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Complete picking (partial or complete)
     */
    public void completePicking() {
        if (status != LicensePlateStatus.PICKED) {
            throw new IllegalStateException("License plate is not being picked");
        }

        if (items.isEmpty()) {
            this.status = LicensePlateStatus.CONSUMED;
        } else {
            this.status = LicensePlateStatus.AT_LOCATION;
        }

        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Pack license plate for shipping
     */
    public void pack() {
        if (status != LicensePlateStatus.AT_LOCATION && status != LicensePlateStatus.ACTIVE) {
            throw new IllegalStateException(
                String.format("Cannot pack license plate in status %s", status));
        }

        this.status = LicensePlateStatus.PACKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Ship license plate
     */
    public void ship() {
        if (status != LicensePlateStatus.PACKED) {
            throw new IllegalStateException("License plate must be packed before shipping");
        }

        this.status = LicensePlateStatus.SHIPPED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Close license plate
     */
    public void close() {
        this.status = LicensePlateStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set owner
     */
    public void setOwner(String ownerId) {
        this.ownerId = ownerId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get items for a specific order
     */
    public List<LPItem> getItemsForOrder(String orderId) {
        return items.stream()
            .filter(item -> item.isForOrder(orderId))
            .collect(Collectors.toList());
    }

    /**
     * Get item by SKU and lot
     */
    public Optional<LPItem> getItem(String sku, String lotNumber) {
        return items.stream()
            .filter(item -> item.matches(sku, lotNumber))
            .findFirst();
    }

    /**
     * Check if license plate is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Check if license plate contains specific SKU
     */
    public boolean containsSku(String sku) {
        return items.stream().anyMatch(item -> item.getSku().equals(sku));
    }

    /**
     * Get latest movement
     */
    public Optional<Movement> getLatestMovement() {
        if (movements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(movements.get(movements.size() - 1));
    }

    /**
     * Get movement history for location
     */
    public List<Movement> getMovementsToLocation(String locationId) {
        return movements.stream()
            .filter(m -> locationId.equals(m.getToLocationId()))
            .collect(Collectors.toList());
    }

    /**
     * Calculate total dwell time at current location
     */
    public long getDwellTimeSeconds() {
        return getLatestMovement()
            .map(m -> java.time.Duration.between(m.getOccurredAt(), LocalDateTime.now()).getSeconds())
            .orElse(0L);
    }

    /**
     * Recalculate totals from items
     */
    private void recalculateTotals() {
        this.totalQuantity = items.stream()
            .mapToInt(LPItem::getQuantity)
            .sum();

        this.totalWeight = items.stream()
            .map(LPItem::getWeight)
            .filter(w -> w != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalVolume = items.stream()
            .map(LPItem::getVolume)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Getters
    public String getLicensePlateId() {
        return licensePlateId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public LicensePlateType getType() {
        return type;
    }

    public LicensePlateStatus getStatus() {
        return status;
    }

    public String getCurrentLocationId() {
        return currentLocationId;
    }

    public String getContainerCode() {
        return containerCode;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public List<LPItem> getItems() {
        return new ArrayList<>(items);
    }

    public List<Movement> getMovements() {
        return new ArrayList<>(movements);
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return String.format("LicensePlate[id=%s, type=%s, status=%s, location=%s, items=%d]",
            licensePlateId, type, status, currentLocationId, items.size());
    }
}
