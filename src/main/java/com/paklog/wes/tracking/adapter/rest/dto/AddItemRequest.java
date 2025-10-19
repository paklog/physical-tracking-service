package com.paklog.wes.tracking.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AddItemRequest(
    @NotBlank(message = "SKU is required")
    String sku,

    String lotNumber,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity,

    BigDecimal weight,

    BigDecimal volume,

    @NotBlank(message = "UOM is required")
    String uom
) {}
