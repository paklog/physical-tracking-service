package com.paklog.wes.tracking.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LPItemTest {

    @Test
    void create_shouldInitializeFields() {
        LPItem item = LPItem.create("SKU-1", "LOT-1", 5,
            new BigDecimal("2.5"), new BigDecimal("1.5"), "EA");

        assertEquals("SKU-1", item.getSku());
        assertEquals("LOT-1", item.getLotNumber());
        assertEquals(5, item.getQuantity());
        assertEquals(new BigDecimal("2.5"), item.getWeight());
        assertEquals(new BigDecimal("1.5"), item.getVolume());
        assertEquals("EA", item.getUom());
        assertNotNull(item.getAddedAt());
    }

    @Test
    void create_shouldRejectInvalidSkuOrQuantity() {
        assertThrows(
            IllegalArgumentException.class,
            () -> LPItem.create(null, "LOT", 1, null, null, "EA")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> LPItem.create("SKU", "LOT", 0, null, null, "EA")
        );
    }

    @Test
    void addAndRemoveQuantity_shouldUpdateCorrectly() {
        LPItem item = LPItem.create("SKU-2", "LOT-2", 2,
            new BigDecimal("1.0"), new BigDecimal("0.4"), "EA");

        item.addQuantity(3);
        assertEquals(5, item.getQuantity());

        item.removeQuantity(4);
        assertEquals(1, item.getQuantity());

        assertThrows(
            IllegalStateException.class,
            () -> item.removeQuantity(2)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> item.addQuantity(0)
        );
    }

    @Test
    void isForOrder_shouldMatchAssignedOrder() {
        LPItem item = LPItem.create("SKU-3", "LOT-3", 1,
            BigDecimal.ONE, BigDecimal.ONE, "EA");
        item.associateWithOrder("ORDER-1");

        assertTrue(item.isForOrder("ORDER-1"));
        assertFalse(item.isForOrder("ORDER-2"));
        assertTrue(item.matches("SKU-3", "LOT-3"));
        assertFalse(item.matches("SKU-3", "LOT-OTHER"));
    }
}
