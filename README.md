# Physical Tracking Service (WES)

Real-time physical tracking of inventory, license plates, and movements.

## Responsibilities

- License plate lifecycle management
- Physical movement tracking
- Real-time location state
- RTLS integration
- Movement history and audit

## Architecture

```
domain/
├── aggregate/      # LicensePlate, LocationState
├── entity/         # Movement, LPItem
├── valueobject/    # LicensePlateType, LPStatus, OccupancyStatus
├── service/        # MovementTrackingService, LicensePlateService
├── repository/     # LicensePlateRepository, LocationStateRepository
└── event/          # LicensePlateMovedEvent, LocationStateChangedEvent

application/
├── command/        # CreateLicensePlateCommand, RecordMovementCommand
├── query/          # GetLocationStateQuery, GetMovementHistoryQuery
└── handler/        # Event handlers

adapter/
├── rest/           # Tracking controllers
└── persistence/    # MongoDB repositories

infrastructure/
├── config/         # Spring configurations
├── messaging/      # Kafka publishers/consumers
└── events/         # Event publishing infrastructure
```

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- MongoDB (movement and state data)
- Apache Kafka (event-driven integration)
- CloudEvents
- OpenAPI/Swagger

## Running the Service

```bash
mvn spring-boot:run
```

## API Documentation

Available at: http://localhost:8085/swagger-ui.html

## Events Published

- `LicensePlateCreatedEvent` - When LP is created
- `LicensePlateMovedEvent` - When LP moves locations
- `LocationStateChangedEvent` - When location occupancy changes
- `LocationBlockedEvent` - When location is blocked
- `PhysicalMovementEvent` - When physical movement occurs

## Events Consumed

- `LocationCreatedEvent` - From Location Master Service
- `LocationCapacityChangedEvent` - From Location Master Service
- `InventoryReceivedEvent` - From Inventory Service
