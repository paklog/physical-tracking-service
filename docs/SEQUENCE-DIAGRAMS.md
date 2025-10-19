# Physical Tracking Service - Sequence Diagrams

## Overview

This document contains sequence diagrams for key operational flows in the Physical Tracking Service.

## 1. Create License Plate and Add Items

This flow shows how a new license plate is created and items are added during receiving or putaway operations.

```mermaid
sequenceDiagram
    participant Client
    participant Controller as PhysicalTrackingController
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository
    participant StateRepo as LocationStateRepository
    participant EventPub as TrackingEventPublisher

    Client->>Controller: POST /license-plates<br/>{warehouseId, type, containerCode}
    Controller->>Service: createLicensePlate(params)

    Service->>LPRepo: save(licensePlate)
    LPRepo-->>Service: LicensePlate

    Service->>EventPub: publishLicensePlateCreated(event)
    EventPub-->>Service: published

    Service-->>Controller: LicensePlate
    Controller-->>Client: 201 Created<br/>{licensePlateId, status: CREATED}

    Note over Client,EventPub: Add Items to License Plate

    Client->>Controller: POST /license-plates/{id}/items<br/>{sku, quantity, weight, volume}
    Controller->>Service: addItemToLicensePlate(lpId, itemDetails)

    Service->>LPRepo: findById(lpId)
    LPRepo-->>Service: LicensePlate

    Service->>Service: lp.addItem(itemDetails)
    Note over Service: Status: CREATED -> ACTIVE<br/>Recalculates totals

    alt License Plate at Location
        Service->>StateRepo: findById(locationId)
        StateRepo-->>Service: LocationState
        Service->>Service: state.updateQuantities()
        Service->>StateRepo: save(state)
    end

    Service->>LPRepo: save(lp)
    LPRepo-->>Service: LicensePlate

    Service->>EventPub: publishItemAdded(event)
    EventPub-->>Service: published

    Service-->>Controller: LicensePlate
    Controller-->>Client: 200 OK<br/>{items: [...], totalQty: 100}
```

## 2. Move License Plate Between Locations

This flow demonstrates the movement of a license plate from one location to another, with automatic location state updates.

```mermaid
sequenceDiagram
    participant Worker
    participant Controller as PhysicalTrackingController
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository
    participant StateRepo as LocationStateRepository
    participant EventPub as TrackingEventPublisher

    Worker->>Controller: POST /license-plates/{id}/move<br/>{toLocationId, movementType, reason}
    Controller->>Service: moveLicensePlate(lpId, toLocationId, type, worker, reason)

    Service->>LPRepo: findById(lpId)
    LPRepo-->>Service: LicensePlate
    Note over Service: fromLocationId = "LOC-A-01"<br/>toLocationId = "LOC-B-05"

    alt From Location Exists
        Service->>StateRepo: findById(fromLocationId)
        StateRepo-->>Service: LocationState (from)

        Service->>Service: fromState.removeLicensePlate(lpId, qty, weight, vol)
        Note over Service: Updates current quantities<br/>Recalculates utilization<br/>Updates occupancy status

        Service->>StateRepo: save(fromState)
        StateRepo-->>Service: saved
    end

    Service->>Service: lp.moveTo(toLocationId, type, worker, reason)
    Note over Service: Creates Movement record<br/>Updates currentLocationId<br/>Status: AT_LOCATION -> IN_TRANSIT -> AT_LOCATION

    Service->>LPRepo: save(lp)
    LPRepo-->>Service: LicensePlate

    alt To Location Exists
        Service->>StateRepo: findById(toLocationId)
        StateRepo-->>Service: LocationState (to)

        Service->>Service: toState.canAccept(qty, weight, vol)?
        alt Has Capacity
            Service->>Service: toState.addLicensePlate(lpId, qty, weight, vol)
            Note over Service: Updates quantities<br/>Recalculates utilization<br/>Updates occupancy status
            Service->>StateRepo: save(toState)
        else Over Capacity
            Service-->>Controller: 409 Conflict<br/>"Location over capacity"
            Controller-->>Worker: Error: Location full
        end
    end

    Service->>EventPub: publishLicensePlateMoved(event)
    EventPub-->>Service: published

    Service-->>Controller: LicensePlate
    Controller-->>Worker: 200 OK<br/>{currentLocationId: "LOC-B-05"}
```

## 3. Pick Items from License Plate

This flow shows picking items from a license plate for order fulfillment.

```mermaid
sequenceDiagram
    participant Picker
    participant Controller as PhysicalTrackingController
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository
    participant StateRepo as LocationStateRepository
    participant EventPub as TrackingEventPublisher

    Note over Picker,EventPub: Start Picking

    Picker->>Controller: POST /license-plates/{id}/start-picking
    Controller->>Service: startPicking(lpId)

    Service->>LPRepo: findById(lpId)
    LPRepo-->>Service: LicensePlate

    Service->>Service: lp.startPicking()
    Note over Service: Status: AT_LOCATION -> PICKED

    Service->>LPRepo: save(lp)
    Service-->>Controller: LicensePlate
    Controller-->>Picker: 200 OK {status: PICKED}

    Note over Picker,EventPub: Pick Items

    Picker->>Controller: DELETE /license-plates/{id}/items<br/>{sku, lotNumber, quantity}
    Controller->>Service: removeItemFromLicensePlate(lpId, sku, lot, qty)

    Service->>LPRepo: findById(lpId)
    LPRepo-->>Service: LicensePlate

    Service->>Service: lp.removeItem(sku, lot, qty)
    Note over Service: Decreases item quantity<br/>Recalculates totals<br/>Removes item if qty=0

    alt All Items Removed
        Service->>Service: Status: PICKED -> CONSUMED
    end

    alt License Plate at Location
        Service->>StateRepo: findById(locationId)
        StateRepo-->>Service: LocationState

        Service->>Service: state.removeLicensePlate(lpId, ...)
        Service->>Service: state.addLicensePlate(lpId, ...) [if not empty]
        Note over Service: Updates location occupancy

        Service->>StateRepo: save(state)
    end

    Service->>LPRepo: save(lp)
    LPRepo-->>Service: LicensePlate

    Service->>EventPub: publishItemRemoved(event)
    EventPub-->>Service: published

    Service-->>Controller: LicensePlate
    Controller-->>Picker: 200 OK<br/>{totalQty: 50, status: PICKED}

    Note over Picker,EventPub: Complete Picking

    Picker->>Controller: POST /license-plates/{id}/complete-picking
    Controller->>Service: completePicking(lpId)

    Service->>LPRepo: findById(lpId)
    LPRepo-->>Service: LicensePlate

    Service->>Service: lp.completePicking()
    Note over Service: If empty: PICKED -> CONSUMED<br/>If partial: PICKED -> AT_LOCATION

    Service->>LPRepo: save(lp)
    Service-->>Controller: LicensePlate
    Controller-->>Picker: 200 OK
```

## 4. Location Blocking and Capacity Management

This flow shows how locations are blocked/unblocked and how capacity constraints are enforced.

```mermaid
sequenceDiagram
    participant Admin
    participant Controller as PhysicalTrackingController
    participant Service as PhysicalTrackingService
    participant StateRepo as LocationStateRepository
    participant EventPub as TrackingEventPublisher

    Note over Admin,EventPub: Block Location

    Admin->>Controller: POST /locations/{id}/block<br/>{reason: "Damaged rack"}
    Controller->>Service: blockLocation(locationId, reason)

    Service->>StateRepo: findById(locationId)
    StateRepo-->>Service: LocationState

    Service->>Service: state.block(reason)
    Note over Service: isBlocked = true<br/>occupancyStatus = BLOCKED<br/>blockReason = "Damaged rack"

    Service->>StateRepo: save(state)
    StateRepo-->>Service: LocationState

    Service->>EventPub: publishLocationBlocked(event)
    EventPub-->>Service: published

    Service-->>Controller: LocationState
    Controller-->>Admin: 200 OK<br/>{status: BLOCKED}

    Note over Admin,EventPub: Attempt to Move LP to Blocked Location

    Admin->>Controller: POST /license-plates/LP-001/move<br/>{toLocationId: blocked-location}
    Controller->>Service: moveLicensePlate(...)

    Service->>LPRepo: findById(lpId)
    Service->>StateRepo: findById(toLocationId)
    StateRepo-->>Service: LocationState {isBlocked: true}

    Service->>Service: state.canAccept(qty, weight, vol)
    Note over Service: Returns false because blocked

    Service-->>Controller: IllegalStateException<br/>"Location is blocked: Damaged rack"
    Controller-->>Admin: 409 Conflict<br/>{error: "Location blocked"}

    Note over Admin,EventPub: Unblock Location

    Admin->>Controller: POST /locations/{id}/unblock
    Controller->>Service: unblockLocation(locationId)

    Service->>StateRepo: findById(locationId)
    StateRepo-->>Service: LocationState

    Service->>Service: state.unblock()
    Note over Service: isBlocked = false<br/>blockReason = null<br/>occupancyStatus recalculated<br/>based on utilization

    Service->>StateRepo: save(state)
    StateRepo-->>Service: LocationState

    Service->>EventPub: publishLocationUnblocked(event)
    EventPub-->>Service: published

    Service-->>Controller: LocationState
    Controller-->>Admin: 200 OK<br/>{status: AVAILABLE}
```

## 5. RTLS Integration and Real-Time Location Updates

This flow shows integration with Real-Time Location Systems for automatic location updates.

```mermaid
sequenceDiagram
    participant RTLS as RTLS System
    participant EventHandler as WarehouseEventHandler
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository
    participant StateRepo as LocationStateRepository

    Note over RTLS,StateRepo: RFID Tag Detected

    RTLS->>EventHandler: LocationUpdateEvent<br/>{tagId: "LP-12345", zone: "PICK-A", coords: {x,y,z}}
    EventHandler->>EventHandler: Parse event

    EventHandler->>Service: updateLicensePlateLocation(containerCode, zone, coords)

    Service->>LPRepo: findByContainerCode(containerCode)
    LPRepo-->>Service: LicensePlate

    alt Location Changed
        Service->>Service: Determine new locationId from zone/coords

        Service->>Service: moveLicensePlate(lpId, newLocationId, RTLS_UPDATE, "SYSTEM", "Auto-detected")
        Note over Service: Follows standard movement flow<br/>Updates from/to location states

        Service->>StateRepo: Save location states
        Service->>LPRepo: Save license plate
    else Location Same
        Service->>Service: Update coordinates only
        Service->>StateRepo: state.setCoordinates(x, y, z, rfidZone)
        Service->>StateRepo: save(state)
    end

    Service-->>EventHandler: Updated
    EventHandler-->>RTLS: ACK

    Note over RTLS,StateRepo: Periodic Location Sync

    loop Every 30 seconds
        RTLS->>EventHandler: BulkLocationUpdate<br/>[{tagId, zone, coords}, ...]

        par Process Updates in Parallel
            EventHandler->>Service: updateLocations(batch)
            Service->>LPRepo: findByContainerCodes(codes)
            Service->>Service: Compare with current locations
            Service->>Service: Update changed locations
            Service->>LPRepo: saveAll(licensePlates)
            Service->>StateRepo: saveAll(locationStates)
        end

        Service-->>EventHandler: Results {updated: 15, unchanged: 85}
    end
```

## 6. Query Location State and License Plates

This flow shows how to query location state and retrieve all license plates at a location.

```mermaid
sequenceDiagram
    participant Client
    participant Controller as PhysicalTrackingController
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository
    participant StateRepo as LocationStateRepository

    Note over Client,StateRepo: Get Location State

    Client->>Controller: GET /locations/{locationId}/state
    Controller->>Service: getLocationState(locationId)

    Service->>StateRepo: findById(locationId)
    StateRepo-->>Service: LocationState or null

    Service-->>Controller: LocationState
    Controller-->>Client: 200 OK<br/>{<br/>  occupancyStatus: "PARTIAL",<br/>  currentQuantity: 450,<br/>  maxQuantity: 1000,<br/>  utilizationPercentage: 45.0,<br/>  licensePlateIds: ["LP-001", "LP-002"],<br/>  isBlocked: false<br/>}

    Note over Client,StateRepo: Get License Plates at Location

    Client->>Controller: GET /locations/{locationId}/license-plates
    Controller->>Service: getLicensePlatesAtLocation(locationId)

    Service->>LPRepo: findByCurrentLocationId(locationId)
    LPRepo-->>Service: List<LicensePlate>

    Service-->>Controller: List<LicensePlate>
    Controller-->>Client: 200 OK<br/>[<br/>  {<br/>    licensePlateId: "LP-001",<br/>    type: "PALLET",<br/>    status: "AT_LOCATION",<br/>    items: [{sku, qty}, ...],<br/>    totalQuantity: 200<br/>  },<br/>  ...<br/>]

    Note over Client,StateRepo: Get License Plate with History

    Client->>Controller: GET /license-plates/{id}
    Controller->>Service: getLicensePlate(lpId)

    Service->>LPRepo: findById(lpId)
    LPRepo-->>Service: LicensePlate

    Service-->>Controller: LicensePlate
    Controller-->>Client: 200 OK<br/>{<br/>  licensePlateId: "LP-001",<br/>  currentLocationId: "LOC-A-05",<br/>  items: [...],<br/>  movements: [<br/>    {<br/>      type: "PUTAWAY",<br/>      fromLocationId: null,<br/>      toLocationId: "LOC-A-05",<br/>      occurredAt: "2025-10-19T10:00:00Z"<br/>    },<br/>    ...<br/>  ]<br/>}
```

## 7. Capacity Alert and Overflow Handling

This flow demonstrates what happens when a location reaches or exceeds capacity.

```mermaid
sequenceDiagram
    participant Worker
    participant Controller as PhysicalTrackingController
    participant Service as PhysicalTrackingService
    participant StateRepo as LocationStateRepository
    participant EventPub as TrackingEventPublisher
    participant AlertSvc as Alerting Service

    Worker->>Controller: POST /license-plates/LP-XXX/move<br/>{toLocationId: "LOC-A-01"}
    Controller->>Service: moveLicensePlate(...)

    Service->>StateRepo: findById("LOC-A-01")
    StateRepo-->>Service: LocationState {<br/>  currentQty: 900,<br/>  maxQty: 1000,<br/>  occupancyStatus: NEARLY_FULL<br/>}

    Service->>Service: state.canAccept(150, weight, vol)
    Note over Service: Check: 900 + 150 = 1050<br/>Max: 1000<br/>Would exceed capacity!

    alt Strict Mode (Reject Overflow)
        Service-->>Controller: IllegalStateException<br/>"Location does not have sufficient capacity"
        Controller-->>Worker: 409 Conflict<br/>{<br/>  error: "CAPACITY_EXCEEDED",<br/>  current: 900,<br/>  max: 1000,<br/>  requested: 150,<br/>  available: 100<br/>}
    else Tolerance Mode (Allow with Warning)
        Service->>Service: state.addLicensePlate(lpId, 150, ...)
        Note over Service: currentQty = 1050<br/>occupancyStatus = OVER_CAPACITY

        Service->>StateRepo: save(state)
        Service->>EventPub: publishCapacityAlert(locationId, utilization: 105%)
        EventPub->>AlertSvc: Send alert
        AlertSvc-->>Worker: Push notification: "Location over capacity"

        Service-->>Controller: LicensePlate (with warning)
        Controller-->>Worker: 200 OK (Warning)<br/>{<br/>  licensePlateId: "LP-XXX",<br/>  warnings: ["Location over capacity: 105%"]<br/>}
    end

    Note over Worker,AlertSvc: Monitor Over-Capacity Locations

    loop Every 5 minutes
        Service->>StateRepo: findOverCapacityLocations(warehouseId)
        StateRepo-->>Service: List<LocationState>

        alt Over-Capacity Found
            Service->>EventPub: publishCapacityReport(locations)
            EventPub->>AlertSvc: Aggregate alert
            AlertSvc-->>AlertSvc: Notify warehouse supervisor
        end
    end
```

## 8. Complete Lifecycle: Receiving to Shipping

This comprehensive flow shows a license plate's journey from creation through shipping.

```mermaid
sequenceDiagram
    participant Receiver
    participant Picker
    participant Packer
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository

    Note over Receiver,LPRepo: 1. RECEIVING

    Receiver->>Service: createLicensePlate(PALLET, "P-001", warehouse)
    Service->>LPRepo: save(lp)
    Note over Service: Status: CREATED

    Receiver->>Service: addItem(lpId, "SKU-001", qty: 100)
    Service->>Service: lp.addItem(...)
    Note over Service: Status: CREATED -> ACTIVE

    Note over Receiver,LPRepo: 2. PUTAWAY

    Receiver->>Service: moveLicensePlate(lpId, "RESERVE-A-01", PUTAWAY)
    Service->>Service: lp.moveTo(...)
    Note over Service: Status: ACTIVE -> AT_LOCATION<br/>Movement: null -> RESERVE-A-01

    Note over Receiver,LPRepo: 3. REPLENISHMENT

    Service->>Service: moveLicensePlate(lpId, "PICK-A-05", REPLENISHMENT)
    Note over Service: Movement: RESERVE-A-01 -> PICK-A-05

    Note over Receiver,LPRepo: 4. PICKING

    Picker->>Service: startPicking(lpId)
    Note over Service: Status: AT_LOCATION -> PICKED

    Picker->>Service: removeItem(lpId, "SKU-001", qty: 30)
    Service->>Service: lp.removeItem(...)
    Note over Service: Item qty: 100 -> 70

    Picker->>Service: completePicking(lpId)
    Note over Service: Status: PICKED -> AT_LOCATION<br/>(partial pick, LP still has items)

    Note over Receiver,LPRepo: 5. CONSOLIDATION

    Service->>Service: moveLicensePlate(lpId, "STAGING-01", TRANSFER)
    Note over Service: Movement: PICK-A-05 -> STAGING-01

    Note over Receiver,LPRepo: 6. PACKING

    Packer->>Service: pack(lpId)
    Note over Service: Status: AT_LOCATION -> PACKED

    Note over Receiver,LPRepo: 7. SHIPPING

    Service->>Service: moveLicensePlate(lpId, "DOCK-03", SHIPPING)
    Service->>Service: lp.ship()
    Note over Service: Status: PACKED -> SHIPPED<br/>Movement: STAGING-01 -> DOCK-03

    Service->>Service: lp.close()
    Note over Service: Status: SHIPPED -> CLOSED<br/>closedAt: timestamp
```

## Error Scenarios

### Scenario 1: Moving Non-Existent License Plate
```mermaid
sequenceDiagram
    participant Client
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository

    Client->>Service: moveLicensePlate("INVALID-LP", ...)
    Service->>LPRepo: findById("INVALID-LP")
    LPRepo-->>Service: Optional.empty()
    Service-->>Client: IllegalArgumentException<br/>"License plate not found: INVALID-LP"
```

### Scenario 2: Adding Items to Shipped License Plate
```mermaid
sequenceDiagram
    participant Client
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository

    Client->>Service: addItemToLicensePlate("LP-001", ...)
    Service->>LPRepo: findById("LP-001")
    LPRepo-->>Service: LicensePlate {status: SHIPPED}
    Service->>Service: lp.addItem(...)<br/>Check: status.canAddItems()
    Service-->>Client: IllegalStateException<br/>"Cannot add items to license plate in status SHIPPED"
```

### Scenario 3: Removing More Items Than Available
```mermaid
sequenceDiagram
    participant Client
    participant Service as PhysicalTrackingService

    Client->>Service: removeItemFromLicensePlate(lpId, "SKU-001", qty: 500)
    Service->>Service: lp.removeItem("SKU-001", qty: 500)
    Service->>Service: item.quantity = 100 (available)<br/>requested = 500
    Service-->>Client: IllegalStateException<br/>"Cannot remove 500 (only 100 available)"
```

## Performance Considerations

### Batch Movement Processing
```mermaid
sequenceDiagram
    participant Client
    participant Service as PhysicalTrackingService
    participant LPRepo as LicensePlateRepository
    participant StateRepo as LocationStateRepository

    Client->>Service: moveLicensePlates([LP-001, LP-002, ...], toLocation)

    par Parallel Processing
        Service->>LPRepo: findAllById([LP-001, LP-002, ...])
        Service->>StateRepo: findById(fromLocation1)
        Service->>StateRepo: findById(toLocation)
    end

    Service->>Service: Validate all LPs can move
    Service->>Service: Check aggregate capacity

    loop For each LP
        Service->>Service: Update LP and location states
    end

    par Batch Save
        Service->>LPRepo: saveAll(licensePlates)
        Service->>StateRepo: saveAll(locationStates)
    end

    Service-->>Client: BatchResult {success: 98, failed: 2}
```

## Event Publishing Patterns

### Async Event Publishing with Retry
```mermaid
sequenceDiagram
    participant Service as PhysicalTrackingService
    participant EventPub as TrackingEventPublisher
    participant Kafka

    Service->>EventPub: publishLicensePlateMoved(event)

    EventPub->>Kafka: send(topic: "tracking.events", event)

    alt Success
        Kafka-->>EventPub: ACK
        EventPub-->>Service: Success
    else Failure
        Kafka-->>EventPub: Error
        EventPub->>EventPub: Retry (attempt 1 of 3)
        EventPub->>Kafka: send(event)

        alt Retry Success
            Kafka-->>EventPub: ACK
            EventPub-->>Service: Success (retried)
        else All Retries Failed
            EventPub->>EventPub: Log to dead letter queue
            EventPub-->>Service: Warning (async failure)
        end
    end
```
