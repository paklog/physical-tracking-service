package com.paklog.wes.tracking.adapter.rest.dto;

import com.paklog.wes.tracking.domain.aggregate.LicensePlate;
import com.paklog.wes.tracking.domain.entity.LPItem;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateStatus;
import com.paklog.wes.tracking.domain.valueobject.LicensePlateType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LicensePlateResponse(
    String licensePlateId,
    String warehouseId,
    LicensePlateType type,
    LicensePlateStatus status,
    String currentLocationId,
    String containerCode,
    Integer totalQuantity,
    BigDecimal totalWeight,
    BigDecimal totalVolume,
    List<LPItemDto> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static LicensePlateResponse from(LicensePlate lp) {
        return new LicensePlateResponse(
            lp.getLicensePlateId(),
            lp.getWarehouseId(),
            lp.getType(),
            lp.getStatus(),
            lp.getCurrentLocationId(),
            lp.getContainerCode(),
            lp.getTotalQuantity(),
            lp.getTotalWeight(),
            lp.getTotalVolume(),
            lp.getItems().stream().map(LPItemDto::from).toList(),
            lp.getCreatedAt(),
            lp.getUpdatedAt()
        );
    }

    public record LPItemDto(
        String itemId,
        String sku,
        String lotNumber,
        Integer quantity,
        BigDecimal weight,
        BigDecimal volume,
        String uom
    ) {
        public static LPItemDto from(LPItem item) {
            return new LPItemDto(
                item.getItemId(),
                item.getSku(),
                item.getLotNumber(),
                item.getQuantity(),
                item.getWeight(),
                item.getVolume(),
                item.getUom()
            );
        }
    }
}
