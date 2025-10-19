package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.valueobject.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MoveLicensePlateRequest(
    @NotBlank(message = "Destination location is required")
    String toLocationId,

    @NotNull(message = "Movement type is required")
    MovementType movementType,

    String reason
) {}
