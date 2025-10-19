package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLicensePlateRequest(
    @NotBlank(message = "License plate ID is required")
    String licensePlateId,

    @NotBlank(message = "Warehouse ID is required")
    String warehouseId,

    @NotNull(message = "Type is required")
    LicensePlateType type,

    String containerCode
) {}
