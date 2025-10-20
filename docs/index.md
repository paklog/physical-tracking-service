---
layout: default
title: Home
---

# Physical Tracking Service Documentation

Real-time inventory location and movement tracking service with license plate management and location state monitoring.

## Overview

The Physical Tracking Service provides real-time tracking of physical inventory movements within the warehouse. It manages License Plates (physical containers like pallets, totes, cartons) and Location States (physical location occupancy and capacity). The service implements event-driven tracking of all inventory movements including putaway, picking, replenishment, transfers, and shipping operations.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for tracking data
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **License Plate Management** - Track pallets, totes, cartons, containers, bins
- **Real-time Location Tracking** - Monitor inventory location changes
- **Movement History** - Complete audit trail of all movements
- **Location State Management** - Track occupancy, capacity, and utilization
- **3D Coordinate Tracking** - Precise warehouse positioning (x, y, z)
- **RFID Zone Integration** - Support for RFID-based tracking
- **Capacity Management** - Monitor and enforce location capacity limits
- **Location Blocking** - Temporary location restrictions

## Domain Model

### Aggregates
- **LicensePlate** - Physical container with items and movement history
- **LocationState** - Physical location occupancy and capacity state

### Entities
- **LPItem** - Individual items within a license plate
- **Movement** - Movement transaction record

### Value Objects
- **LicensePlateType** - Container type specifications
- **LicensePlateStatus** - Container lifecycle status
- **MovementType** - Movement operation classification
- **OccupancyStatus** - Location occupancy level

### License Plate Lifecycle

```
CREATED -> ACTIVE -> AT_LOCATION -> IN_TRANSIT -> PICKED ->
PACKED -> SHIPPED -> CONSUMED -> CLOSED
```

### Movement Types

- **PUTAWAY** - Inventory put into storage
- **PICK** - Inventory picked from location
- **REPLENISHMENT** - Move inventory to pick locations
- **TRANSFER** - Move between locations
- **CONSOLIDATION** - Combine license plates
- **RELOCATION** - Move to different location
- **CYCLE_COUNT** - Inventory count verification
- **ADJUSTMENT** - Inventory correction
- **SHIPPING** - Outbound shipment
- **RECEIVING** - Inbound receipt

## Domain Events

### Published Events
- **LicensePlateCreated** - New license plate created
- **LicensePlateMoved** - License plate moved to new location
- **ItemAddedToLicensePlate** - Item added to container
- **ItemRemovedFromLicensePlate** - Item removed from container
- **LicensePlatePicked** - License plate picked
- **LicensePlatePacked** - License plate packed
- **LicensePlateShipped** - License plate shipped
- **LicensePlateClosed** - License plate closed
- **LocationStateChanged** - Location occupancy changed
- **LocationBlocked** - Location blocked
- **LocationUnblocked** - Location unblocked
- **LocationCapacityExceeded** - Location over capacity

### Consumed Events
- **PickConfirmed** - Update license plate on pick
- **PutawayCompleted** - Create/update license plate
- **ReplenishmentCompleted** - Track replenishment movement
- **ShipmentDispatched** - Mark license plates as shipped
- **ReceiptCreated** - Create inbound license plates

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Real-time movement tracking via events
- **Aggregate Pattern** - License plate and location state boundaries
- **Event Sourcing** - Complete movement history tracking

## API Endpoints

### License Plate Management
- `POST /license-plates` - Create license plate
- `GET /license-plates/{licensePlateId}` - Get license plate details
- `PUT /license-plates/{licensePlateId}/move` - Move to location
- `POST /license-plates/{licensePlateId}/items` - Add item
- `DELETE /license-plates/{licensePlateId}/items/{itemId}` - Remove item
- `POST /license-plates/{licensePlateId}/close` - Close license plate
- `GET /license-plates` - List license plates with filtering

### License Plate Operations
- `GET /license-plates/{licensePlateId}/movements` - Get movement history
- `GET /license-plates/location/{locationId}` - Get LPs at location
- `GET /license-plates/container/{containerCode}` - Find by container code
- `POST /license-plates/{licensePlateId}/pick` - Mark as picked
- `POST /license-plates/{licensePlateId}/pack` - Mark as packed
- `POST /license-plates/{licensePlateId}/ship` - Mark as shipped

### Location State Management
- `GET /locations/{locationId}/state` - Get location state
- `POST /locations/{locationId}/block` - Block location
- `POST /locations/{locationId}/unblock` - Unblock location
- `GET /locations/warehouse/{warehouseId}` - Get locations by warehouse
- `GET /locations/zone/{zone}` - Get locations by zone
- `GET /locations/over-capacity` - Find over-capacity locations

### Tracking Queries
- `GET /tracking/license-plate/{licensePlateId}` - Track license plate
- `GET /tracking/location/{locationId}` - Track location contents
- `GET /tracking/sku/{sku}` - Find SKU locations
- `GET /tracking/movements` - Query movement history

## License Plate Types

### PALLET
- Large capacity container
- Max weight: 2000 lbs
- Max volume: 96 cubic feet
- Typical for bulk storage

### TOTE
- Medium capacity container
- Max weight: 50 lbs
- Max volume: 3 cubic feet
- Used for order fulfillment

### CARTON
- Small to medium container
- Max weight: 70 lbs
- Max volume: 10 cubic feet
- Used for outbound shipments

### CONTAINER
- Extra large capacity
- Max weight: 5000 lbs
- Max volume: 200 cubic feet
- Used for bulk receiving

### BIN
- Small capacity container
- Max weight: 25 lbs
- Max volume: 1 cubic foot
- Used for small parts

## Location Occupancy Status

### EMPTY
Location has no license plates (0% utilization).

### AVAILABLE
Location has space for more license plates (1-25% utilization).

### PARTIAL
Location is partially filled (26-60% utilization).

### NEARLY_FULL
Location is mostly filled (61-90% utilization).

### FULL
Location is at capacity (91-100% utilization).

### OVER_CAPACITY
Location exceeds capacity limits (>100% utilization).

### BLOCKED
Location is temporarily blocked and unavailable.

## Capacity Management

### Location Capacity Tracking
- **Quantity Capacity** - Maximum number of items
- **Weight Capacity** - Maximum total weight
- **Volume Capacity** - Maximum total volume
- **License Plate Capacity** - Maximum number of containers

### Capacity Enforcement
- Prevent moves to over-capacity locations
- Alert on approaching capacity limits
- Automatic status updates based on utilization
- Support for temporary capacity overrides

## Movement Tracking

### Movement Attributes
- Movement ID (unique identifier)
- Movement type (putaway, pick, etc.)
- From location
- To location
- Performed by (operator ID)
- Occurred at (timestamp)
- Reason (optional description)
- Associated task ID
- Associated wave ID

### Movement History
Complete audit trail of all license plate movements:
- Chronological movement sequence
- Location transitions
- Operator accountability
- Task and wave associations
- Movement reasons and context

## 3D Location Tracking

### Coordinate System
- **X Coordinate** - Horizontal position (aisle direction)
- **Y Coordinate** - Horizontal position (bay direction)
- **Z Coordinate** - Vertical position (height)

### RFID Zone Integration
- RFID zone assignment per location
- Real-time RFID event processing
- Location verification via RFID
- Automatic movement detection

## Integration Points

### Consumes Events From
- Task Execution (task completed, movements)
- Pick Execution (picks confirmed)
- Pack Ship (items packed, shipped)
- Receiving (receipts created)
- Inventory (adjustments, cycle counts)

### Publishes Events To
- Inventory (inventory movements)
- Task Execution (location status changes)
- Location Master (occupancy updates)
- Warehouse Analytics (movement metrics)

## Performance Considerations

### Database Optimization
- MongoDB indexes on licensePlateId, locationId, warehouseId
- Compound indexes on status + warehouseId + currentLocationId
- Index on containerCode for barcode lookups
- TTL index on closed license plates (archive after 90 days)

### Caching Strategy
- Location state cached for 5 minutes
- License plate status cached for 1 minute
- Movement history paginated for performance
- Aggregate metrics pre-calculated

### Event Processing
- Asynchronous movement event processing
- Batch processing for bulk movements
- Event deduplication
- Retry logic for failed events

## Business Rules

1. **License Plate Rules**
   - Container code must be unique per warehouse
   - Cannot add items to closed license plates
   - Cannot move shipped license plates
   - Movement history is immutable

2. **Location State Rules**
   - Cannot exceed configured capacity limits
   - Blocked locations cannot accept license plates
   - Location must exist in Location Master
   - Coordinates must be within warehouse bounds

3. **Movement Rules**
   - From and to locations must be valid
   - Movement type must match operation
   - Operator must be authenticated
   - Task association required for task movements

4. **Item Management Rules**
   - Items cannot exceed license plate capacity
   - SKU and lot matching enforced
   - Quantity must be positive
   - Cannot remove more than available

## Monitoring and Metrics

### Key Metrics
- Total active license plates
- License plates by status
- Location utilization percentage
- Movements per hour
- Over-capacity location count
- Blocked location count

### Health Indicators
- Event processing lag
- Movement tracking accuracy
- Location state sync status
- Database performance metrics

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `tracking.archive.closed-lp-days` - Days before archiving closed LPs (default: 90)
- `tracking.cache.location-state-ttl` - Location state cache TTL (default: 5m)
- `tracking.cache.lp-status-ttl` - License plate cache TTL (default: 1m)
- `tracking.movement.batch-size` - Batch processing size (default: 100)
- `tracking.capacity.alert-threshold` - Capacity alert threshold % (default: 90)
- `tracking.rfid.enabled` - Enable RFID integration (default: false)

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: WMS Team
- **Slack**: #wms-physical-tracking
