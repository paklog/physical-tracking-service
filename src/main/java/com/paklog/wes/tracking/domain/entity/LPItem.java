package com.paklog.wes.tracking.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * License Plate Item - Individual item on a license plate
 */
public class LPItem {
    private String itemId;
    private String sku;
    private String lotNumber;
    private Integer quantity;
    private BigDecimal weight;
    private BigDecimal volume;
    private String uom; // Unit of measure
    private LocalDateTime addedAt;
    private String orderId; // If picked for order
    private String taskId; // Associated task

    protected LPItem() {
        // For persistence
    }

    public LPItem(String sku, String lotNumber, Integer quantity,
                  BigDecimal weight, BigDecimal volume, String uom) {
        this.itemId = java.util.UUID.randomUUID().toString();
        this.sku = sku;
        this.lotNumber = lotNumber;
        this.quantity = quantity;
        this.weight = weight;
        this.volume = volume;
        this.uom = uom;
        this.addedAt = LocalDateTime.now();
    }

    public static LPItem create(String sku, String lotNumber, Integer quantity,
                               BigDecimal weight, BigDecimal volume, String uom) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        return new LPItem(sku, lotNumber, quantity, weight, volume, uom);
    }

    /**
     * Add quantity to item
     */
    public void addQuantity(int additionalQty) {
        if (additionalQty <= 0) {
            throw new IllegalArgumentException("Additional quantity must be positive");
        }
        this.quantity += additionalQty;
    }

    /**
     * Remove quantity from item
     */
    public void removeQuantity(int qtyToRemove) {
        if (qtyToRemove <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }
        if (qtyToRemove > this.quantity) {
            throw new IllegalStateException(
                String.format("Cannot remove %d (only %d available)", qtyToRemove, this.quantity));
        }
        this.quantity -= qtyToRemove;
    }

    /**
     * Associate with order
     */
    public void associateWithOrder(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Associate with task
     */
    public void associateWithTask(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Check if item is for a specific order
     */
    public boolean isForOrder(String orderId) {
        return orderId != null && orderId.equals(this.orderId);
    }

    /**
     * Check if item matches SKU and lot
     */
    public boolean matches(String sku, String lotNumber) {
        boolean skuMatch = this.sku.equals(sku);
        boolean lotMatch = (this.lotNumber == null && lotNumber == null) ||
                          (this.lotNumber != null && this.lotNumber.equals(lotNumber));
        return skuMatch && lotMatch;
    }

    // Getters
    public String getItemId() {
        return itemId;
    }

    public String getSku() {
        return sku;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public String getUom() {
        return uom;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public String toString() {
        return String.format("LPItem[sku=%s, lot=%s, qty=%d, order=%s]",
            sku, lotNumber, quantity, orderId);
    }
}
