# Physical Tracking Service

Real-time license plate tracking, movement management, and location state monitoring for complete warehouse visibility.

## Overview

The Physical Tracking Service provides comprehensive real-time tracking of physical assets throughout the warehouse. This bounded context manages the complete lifecycle of license plates (pallets, totes, cartons), tracks all physical movements between locations, maintains current location state and occupancy, provides movement history and audit trails, and integrates with RTLS (Real-Time Location Systems) for automated tracking. The service serves as the single source of truth for "where is it now" queries across warehouse operations.

## Domain-Driven Design

### Bounded Context
**Physical Asset Tracking & Movement** - Manages real-time location and movement of physical containers and inventory throughout the warehouse.

### Core Domain Model

#### Aggregates
- **LicensePlate** - Root aggregate representing a physical container
- **LocationState** - Root aggregate representing current state of a location

#### Entities
- **LPItem** - Item within a license plate
- **Movement** - Physical movement record

#### Value Objects
- **LicensePlateType** - Container type (PALLET, TOTE, CARTON, CASE, EACHES_CONTAINER)
- **LicensePlateStatus** - LP lifecycle status (CREATED, ACTIVE, AT_LOCATION, IN_TRANSIT, PICKED, PACKED, SHIPPED, CONSUMED, CLOSED)
- **MovementType** - Movement category (RECEIVE, PUTAWAY, PICK, REPLENISH, TRANSFER, SHIP, CYCLE_COUNT)
- **OccupancyStatus** - Location state (EMPTY, PARTIALLY_OCCUPIED, FULL, BLOCKED)

#### Domain Events
- **LicensePlateCreatedEvent** - New license plate created
- **LicensePlateMovedEvent** - License plate moved to new location
- **ItemAddedToLPEvent** - Item added to license plate
- **ItemRemovedFromLPEvent** - Item removed from license plate
- **LocationStateChangedEvent** - Location occupancy state changed
- **LicensePlateConsumedEvent** - License plate emptied
- **LicensePlateClosedEvent** - License plate lifecycle ended

### Ubiquitous Language
- **License Plate**: Physical container holding inventory (pallet, tote, carton)
- **License Plate ID**: Unique barcode identifier for container
- **Location State**: Current occupancy and status of warehouse location
- **Movement**: Physical transfer from one location to another
- **Movement History**: Complete audit trail of container movements
- **Dwell Time**: Duration container remains at a location
- **In Transit**: Container actively being moved
- **Consumed**: Container emptied of all contents
- **RTLS**: Real-Time Location System for automated tracking

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wes/tracking/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   ├── LicensePlate.java        # LP aggregate root
│   │   └── LocationState.java       # Location state aggregate
│   ├── entity/                      # Entities
│   │   ├── LPItem.java             # Item in license plate
│   │   └── Movement.java            # Movement record
│   ├── valueobject/                 # Value objects
│   │   ├── LicensePlateType.java
│   │   ├── LicensePlateStatus.java
│   │   ├── MovementType.java
│   │   └── OccupancyStatus.java
│   ├── repository/                  # Repository interfaces
│   │   ├── LicensePlateRepository.java
│   │   └── LocationStateRepository.java
│   ├── service/                     # Domain services
│   │   ├── MovementTrackingService.java
│   │   └── OccupancyCalculator.java
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   │   └── PhysicalTrackingService.java
│   ├── command/                     # Commands
│   │   ├── CreateLicensePlateCommand.java
│   │   ├── MoveLicensePlateCommand.java
│   │   ├── AddItemCommand.java
│   │   └── RemoveItemCommand.java
│   └── query/                       # Queries
└── adapter/                          # External adapters
    ├── rest/                        # REST controllers
    │   └── PhysicalTrackingController.java
    ├── persistence/                 # MongoDB repositories
    └── events/                      # Event publishers/consumers
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain models for tracking
- **Event Sourcing Pattern** - Complete movement history
- **Aggregate Pattern** - Consistency boundaries around license plates
- **Event-Driven Architecture** - Real-time movement notifications
- **Repository Pattern** - Data access abstraction
- **CQRS Pattern** - Optimized read models for location queries
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for LP and movement data
- **Spring Data MongoDB** - Data access layer

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Loki Logback Appender** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Real-time tracking patterns

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven movement tracking
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/physical-tracking-service.git
   cd physical-tracking-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8085/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f physical-tracking-service

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8085/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8085/v3/api-docs

### Key Endpoints

#### License Plate Management
- `POST /license-plates` - Create new license plate
- `GET /license-plates/{lpId}` - Get license plate details
- `POST /license-plates/{lpId}/move` - Move license plate to location
- `POST /license-plates/{lpId}/items` - Add item to license plate
- `DELETE /license-plates/{lpId}/items/{itemId}` - Remove item from license plate
- `POST /license-plates/{lpId}/close` - Close license plate
- `GET /license-plates/location/{locationId}` - Get LPs at location

#### Location State Queries
- `GET /locations/{locationId}/state` - Get current location state
- `GET /locations/{locationId}/occupancy` - Get occupancy details
- `GET /locations/{locationId}/license-plates` - List LPs at location

#### Movement Tracking
- `GET /license-plates/{lpId}/movements` - Get movement history
- `GET /license-plates/{lpId}/current-location` - Get current location
- `GET /movements/recent` - Get recent movements
- `GET /movements/by-type/{movementType}` - Query by movement type

## Tracking Features

### Real-Time Location Tracking

- **Current Location**: Immediate "where is it now" queries
- **Movement History**: Complete audit trail of all movements
- **Dwell Time Calculation**: How long at current location
- **In-Transit Tracking**: Items actively being moved
- **Multi-Level Nesting**: Track pallets containing totes containing cartons

### License Plate Lifecycle

```
CREATED → ACTIVE → AT_LOCATION ⟷ IN_TRANSIT → PICKED → PACKED → SHIPPED → CONSUMED → CLOSED
```

### Movement Types

- **RECEIVE**: Inbound receiving to dock
- **PUTAWAY**: Move from receiving to storage
- **PICK**: Remove from storage for order
- **REPLENISH**: Move from reserve to pick location
- **TRANSFER**: Internal location transfer
- **SHIP**: Outbound to carrier
- **CYCLE_COUNT**: Movement for inventory count

### Location State Management

- **EMPTY**: No license plates present
- **PARTIALLY_OCCUPIED**: Has LPs, capacity available
- **FULL**: At maximum capacity
- **BLOCKED**: Location unavailable for operations

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/physical_tracking
  kafka:
    bootstrap-servers: localhost:9092

physical-tracking:
  movement:
    audit-retention-days: 365
    enable-location-state-sync: true
  license-plate:
    auto-consume-when-empty: true
    track-nested-containers: true
```

## Event Integration

### Published Events
- `com.paklog.wes.tracking.lp.created.v1`
- `com.paklog.wes.tracking.lp.moved.v1`
- `com.paklog.wes.tracking.lp.item.added.v1`
- `com.paklog.wes.tracking.lp.item.removed.v1`
- `com.paklog.wes.tracking.lp.consumed.v1`
- `com.paklog.wes.tracking.lp.closed.v1`
- `com.paklog.wes.tracking.location.state.changed.v1`

### Consumed Events
- `com.paklog.wms.location.created.v1` - Initialize location state
- `com.paklog.inventory.received.v1` - Create receiving license plates
- `com.paklog.wes.pick.confirmed.v1` - Record pick movements
- `com.paklog.wes.pack.item.packed.v1` - Update LP status for packing
- `com.paklog.shipment.dispatched.v1` - Mark LPs as shipped

### Event Format
All events follow the CloudEvents specification v1.0 and are published asynchronously via Kafka.

## Monitoring

- **Health**: http://localhost:8085/actuator/health
- **Metrics**: http://localhost:8085/actuator/metrics
- **Prometheus**: http://localhost:8085/actuator/prometheus
- **Info**: http://localhost:8085/actuator/info

### Key Metrics
- `lp.created.total` - Total license plates created
- `lp.movements.total` - Total movements recorded
- `lp.active.count` - Currently active license plates
- `location.occupancy.percentage` - Average location utilization
- `movement.dwell.time.seconds` - Average dwell time per location
- `lp.consumed.rate` - License plate consumption rate

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Maintain complete movement audit trails
4. Track license plate lifecycle accurately
5. Provide real-time location state
6. Write comprehensive tests including movement scenarios
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
