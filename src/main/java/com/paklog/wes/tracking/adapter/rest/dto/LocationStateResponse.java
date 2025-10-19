package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.aggregate.LocationState;
import com.paklog.wes.tracking.domain.valueobject.OccupancyStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LocationStateResponse(
    String locationId,
    String warehouseId,
    String zone,
    OccupancyStatus occupancyStatus,
    List<String> licensePlateIds,
    Integer maxQuantity,
    Integer currentQuantity,
    BigDecimal maxWeight,
    BigDecimal currentWeight,
    BigDecimal maxVolume,
    BigDecimal currentVolume,
    BigDecimal utilizationPercentage,
    Boolean isBlocked,
    String blockReason,
    LocalDateTime lastUpdated
) {
    public static LocationStateResponse from(LocationState state) {
        return new LocationStateResponse(
            state.getLocationId(),
            state.getWarehouseId(),
            state.getZone(),
            state.getOccupancyStatus(),
            state.getLicensePlateIds(),
            state.getMaxQuantity(),
            state.getCurrentQuantity(),
            state.getMaxWeight(),
            state.getCurrentWeight(),
            state.getMaxVolume(),
            state.getCurrentVolume(),
            state.getUtilizationPercentage(),
            state.getIsBlocked(),
            state.getBlockReason(),
            state.getLastUpdated()
        );
    }
}
