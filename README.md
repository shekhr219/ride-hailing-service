# Ride Hailing Service Backend

A production-quality, modular monolith backend for a ride-hailing service (similar to Uber) built with **Java 17/18** and **Spring Boot 3**.

---

## 1. Overview

The **Ride Hailing Service Backend** manages riders, drivers, ride dispatching, geolocation tracking, tiered pricing, and coupon discounts. It is designed around clean object-oriented principles and the **Strategy Pattern** to ensure extensibility, thread-safety, and testability.

### Core Capabilities:
- **User & Driver Management:** Driver status tracking (`AVAILABLE`, `ON_RIDE`), vehicle classification (`HATCHBACK`, `SEDAN`), and rating system.
- **Intelligent Dispatch:** Radius-based driver matching (`NearestDriverStrategy`) with thread-safe atomic assignment.
- **Hatchback Fallback Rule:** Automatic fallback to an available Sedan at standard Sedan pricing without upgrade penalties when no Hatchback is nearby, maintaining both `requestedCarType` and `actualCarType`.
- **Tiered Fare Calculation:** Distance-based pricing tiers with minimum fare guarantees.
- **Coupon & Promotion Engine:** Strategy-based discount calculation supporting percentage and flat discounts, with safeguards against negative fares.
- **Ride Lifecycle & History:** Seamless state management (`ONGOING` -> `COMPLETED` or `CANCELLED`) with audit trails and user/driver ride history.

---

## 2. Architecture

The application adheres to a **Modular Monolith / Layered Architecture** with strict separation of concerns:

```
┌────────────────────────────────────────────────────────┐
│                   Controller / API Layer               │
│   (UserController, DriverController, RideController,   │
│                   CouponController)                    │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                      Service Layer                     │
│    (UserService, DriverService, RideService,           │
│                    CouponService)                      │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
┌─────────────▼─────────────┐ ┌────────────▼─────────────┐
│      Strategy Layer       │ │    Domain / Utility      │
│ - DriverMatchingStrategy  │ │ - User, Driver, Ride     │
│ - PricingStrategy         │ │ - Coupon, Location       │
│ - DiscountStrategy        │ │ - Haversine Calculator   │
└─────────────┬─────────────┘ └──────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────┐
│                   Repository Layer                     │
│  - Interfaces: UserRepository, DriverRepository,       │
│                RideRepository, CouponRepository        │
│  - Implementations: Thread-Safe In-Memory Repositories │
└────────────────────────────────────────────────────────┘
```

### Layer Responsibilities:
1. **API / Controller Layer:** Validates HTTP payloads (`@Valid`, Bean Validation), handles routing, and returns standard HTTP status codes and responses.
2. **Service Layer:** Coordinates business workflows, transactions, domain invariants, and lifecycle transitions.
3. **Strategy Layer:** Encapsulates dynamic business algorithms (driver matching, pricing tiers, discounts) using the Strategy Pattern.
4. **Domain & Util Layer:** Encapsulates core state, thread-safe transitions (`Driver.tryAssign()`), and geographic calculations (`HaversineDistanceCalculator`).
5. **Repository Layer:** Decoupled interfaces backed by `ConcurrentHashMap` for high-throughput, thread-safe in-memory operations.

---

## 3. Domain Model

- **`User`:** Represents a rider (`id`, `name`, `phone`).
- **`Driver`:** Represents a registered driver (`id`, `name`, `phone`, `carType`, `rating`, `currentLocation`, `status`). Contains thread-safe atomic transition methods (`tryAssign()`, `release()`, `updateLocation()`).
- **`Location`:** Immutable value object representing geographic coordinates (`latitude`, `longitude`) with latitude $[-90, 90]$ and longitude $[-180, 180]$ validation.
- **`Ride`:** Represents the ride entity (`id`, `userId`, `driverId`, `pickupLocation`, `dropoffLocation`, `requestedCarType`, `actualCarType`, `status`, `distanceKm`, `fare`, `couponCode`, `discountAmount`, `finalFare`, `cancellationFee`, `createdAt`, `completedAt`).
- **`Coupon`:** Promotional coupon (`code`, `discountType`, `discountValue`, `active`).

---

## 4. Design Decisions

### 1. In-Memory Repositories
- **Why:** The specification strictly prohibits external databases (PostgreSQL, Redis, Kafka) to keep setup time and complexity minimal.
- **Thread Safety:** Implemented with `ConcurrentHashMap` and thread-safe collections. Repository interfaces mirror Spring Data semantics, enabling zero-friction migration to JPA/Hibernate if needed.

### 2. Strategy Pattern for Driver Matching (`DriverMatchingStrategy`)
- Encapsulates how a driver is chosen.
- `NearestDriverStrategy` (default, `@Primary`) finds eligible drivers within radius, sorted by shortest distance.
- `HighestRatedDriverStrategy` is also provided as a swappable strategy demonstrating zero-touch extensibility for RideService.

### 3. Separation of `requestedCarType` and `actualCarType`
- Preserves user intent for audits, analytics, and driver assignment transparency.
- When the Hatchback fallback rule triggers, `requestedCarType` remains `HATCHBACK` while `actualCarType` is `SEDAN`.

### 4. Strategy Pattern for Pricing (`PricingStrategy`)
- Hardcoding rates in `RideService` violates the Open/Closed Principle.
- `TieredPricingStrategy` encapsulates distance slab calculations and minimum fare boundaries for different car categories (`SEDAN`, `HATCHBACK`).

### 5. Strategy Pattern for Discounts (`DiscountStrategy`)
- Enables extending promotions (`PercentageDiscountStrategy`, `FlatDiscountStrategy`) without modifying coupon validation services.
- `DiscountStrategyResolver` dynamically resolves the applicable strategy based on `DiscountType`.

---

## 5. Assumptions

1. **Pricing Model:**
   - **SEDAN:**
     - Minimum Fare: ₹50.00
     - Tier 1 (0 to 2 km): ₹10.00/km
     - Tier 2 (2 to 5 km): ₹8.00/km
     - Tier 3 (> 5 km): ₹5.00/km
   - **HATCHBACK:**
     - Minimum Fare: ₹40.00
     - Tier 1 (0 to 2 km): ₹8.00/km
     - Tier 2 (2 to 5 km): ₹6.00/km
     - Tier 3 (> 5 km): ₹4.00/km
   - *Rounding:* All fares and discounts are rounded to 2 decimal places using `RoundingMode.HALF_UP`.
   - *Minimum Fare Rule:* If the distance calculation yields an amount lower than the minimum fare, the minimum fare applies.
2. **Matching Radius:** Default is 5.0 km, configurable via `ride.matching.radius-km` in `application.properties`.
3. **Distance Calculation:** Haversine formula assuming Earth radius $R = 6371.0 \text{ km}$.
4. **Hatchback Fallback Pricing:** When a user requests a Hatchback and receives a Sedan due to fallback, the user is charged the Sedan normal rate (with no extra upgrade penalty fee), as specified in requirements.
5. **Coupons:**
   - Case-insensitive codes (e.g., `SAVE20` matches `save20`).
   - Percentage discounts are capped at $100\%$ during creation and cannot make the final fare negative.
6. **Ride Cancellation:** Cancelling an ongoing ride releases the driver back to `AVAILABLE` and charges a flat cancellation fee of ₹25.00.

---

## 6. Concurrency Safety

### In-Memory Solution
A classic race condition occurs when two concurrent booking requests at the same location identify the same nearest driver simultaneously:

1. **Atomic State Transition:** The `Driver` class provides a synchronized `tryAssign()` method:
   ```java
   public synchronized boolean tryAssign() {
       if (this.status == DriverStatus.AVAILABLE) {
           this.status = DriverStatus.ON_RIDE;
           return true;
       }
       return false;
   }
   ```
2. **Contention Handling:** Inside `NearestDriverStrategy`, candidate drivers are sorted by proximity. The matching engine iterates through the sorted candidates and attempts `driver.tryAssign()`. If thread A claims the driver, thread B's attempt returns `false` and immediately falls through to the next nearest candidate.
3. **Verification:** Verified with multi-threaded stress tests (`ConcurrencyTest`) using `ExecutorService` and `CountDownLatch`.

### Production Distributed System (Database / Multi-Instance)
In a multi-instance microservices environment with a persistent database, in-memory Java synchronization is insufficient. We would use:
- **Pessimistic Locking:** `SELECT * FROM drivers WHERE id = :id FOR UPDATE` inside a database transaction.
- **Optimistic Locking:** An `@Version` column on the Driver table where `UPDATE drivers SET status = 'ON_RIDE', version = version + 1 WHERE id = :id AND status = 'AVAILABLE' AND version = :version`.
- **Distributed Lock:** Redis Redlock or ZooKeeper lock keyed on `lock:driver:{driverId}`.

---

## 7. API Reference & Examples

### 1. Register User
`POST /users`
```json
// Request
{
  "name": "Aditi Sharma",
  "phone": "9876543210"
}

// Response (201 Created)
{
  "id": "USER-c12e4f09",
  "name": "Aditi Sharma",
  "phone": "9876543210"
}
```

### 2. Register Driver
`POST /drivers`
```json
// Request
{
  "name": "Ramesh Kumar",
  "phone": "9876543211",
  "carType": "SEDAN",
  "rating": 4.9,
  "latitude": 12.9716,
  "longitude": 77.5946
}

// Response (201 Created)
{
  "id": "DRV-84ba5c02",
  "name": "Ramesh Kumar",
  "phone": "9876543211",
  "carType": "SEDAN",
  "rating": 4.9,
  "currentLocation": {
    "latitude": 12.9716,
    "longitude": 77.5946
  },
  "status": "AVAILABLE"
}
```

### 3. Update Driver Location
`PATCH /drivers/{driverId}/location`
```json
// Request
{
  "latitude": 12.9730,
  "longitude": 77.5960
}

// Response (200 OK)
{
  "id": "DRV-84ba5c02",
  "status": "AVAILABLE",
  "currentLocation": {
    "latitude": 12.973,
    "longitude": 77.596
  }
}
```

### 4. Create Coupon
`POST /coupons`
```json
// Request
{
  "code": "RIDE20",
  "discountType": "PERCENTAGE",
  "discountValue": 20.0,
  "active": true
}

// Response (201 Created)
{
  "code": "RIDE20",
  "discountType": "PERCENTAGE",
  "discountValue": 20.00,
  "active": true
}
```

### 5. Book Ride
`POST /rides`
```json
// Request
{
  "userId": "USER-c12e4f09",
  "pickupLatitude": 12.9720,
  "pickupLongitude": 77.5950,
  "requestedCarType": "HATCHBACK"
}

// Response (201 Created) - Note Hatchback fallback to Sedan if Hatchback unavailable
{
  "rideId": "RIDE-5a3d72bc",
  "userId": "USER-c12e4f09",
  "driverId": "DRV-84ba5c02",
  "pickupLocation": {
    "latitude": 12.972,
    "longitude": 77.595
  },
  "requestedCarType": "HATCHBACK",
  "actualCarType": "SEDAN",
  "status": "ONGOING"
}
```

### 6. End Ride
`POST /rides/{rideId}/end`
```json
// Request
{
  "driverId": "DRV-84ba5c02",
  "dropoffLatitude": 13.0346,
  "dropoffLongitude": 77.5950,
  "couponCode": "RIDE20"
}

// Response (200 OK)
{
  "rideId": "RIDE-5a3d72bc",
  "userId": "USER-c12e4f09",
  "driverId": "DRV-84ba5c02",
  "status": "COMPLETED",
  "distanceKm": 6.96,
  "fare": 53.80,
  "couponCode": "RIDE20",
  "discountAmount": 10.76,
  "finalFare": 43.04
}
```

### 7. Cancel Ride
`POST /rides/{rideId}/cancel`
```json
// Response (200 OK)
{
  "rideId": "RIDE-5a3d72bc",
  "status": "CANCELLED",
  "cancellationFee": 25.00,
  "finalFare": 25.00
}
```

### 8. Ride History
- `GET /users/{userId}/rides?status=COMPLETED`
- `GET /drivers/{driverId}/rides?status=ONGOING`

---

## 8. Testing Summary

A comprehensive test suite of **47 automated tests** covers all business rules:

| Test Suite | Tests | Description |
|---|---|---|
| **TieredPricingStrategyTest** | 13 | Validates minimum fare, 2 km boundary, 5 km boundary, >5 km slab, 0 km ride, negative input handling for both Sedan and Hatchback. |
| **CouponStrategyAndServiceTest** | 11 | Tests percentage discounts, flat discounts, non-negative cap, duplicate code check, inactive coupon, delete, and invalid coupon handling. |
| **DriverMatchingStrategyTest** | 7 | Verifies nearest driver selection, radius filtering, ignoring unavailable drivers, Hatchback priority, Hatchback-to-Sedan fallback, and HighestRatedDriverStrategy. |
| **RideServiceTest** | 11 | Full lifecycle testing: booking, state updates, ride completion, driver release, coupon discounts, ride history, cancellation, and validation errors. |
| **ConcurrencyTest** | 2 | Multi-threaded stress testing using `CountDownLatch`: verifies race conditions on 1 available driver with 10 threads, and 5 drivers with 15 concurrent threads. |
| **RideHailingIntegrationTest** | 4 | End-to-end Spring Boot `MockMvc` integration tests verifying all REST endpoints, headers, JSON formats, and error responses. |

**Total:** 47 tests, 0 failures, 0 errors, 0 skipped.

---

## 9. How to Build and Run

### Prerequisites
- Java 17 or higher (`java -version`)
- Maven (or use bundled `./mvnw.bat`)

### Running the Test Suite
```bash
./mvnw.bat test
```

### Building the Executable JAR
```bash
./mvnw.bat package -DskipTests
```

### Running the Application
```bash
./mvnw.bat spring-boot:run
```
Or execute the packaged jar directly:
```bash
java -jar target/ride-hailing-service-1.0.0.jar
```
The server will start on port `8080` (`http://localhost:8080`).

---

## 10. What I Would Improve With More Time

1. **Persistent Storage:** Replace in-memory storage with PostgreSQL / MySQL using Spring Data JPA with Flyway database migrations.
2. **Geospatial Indexing:** Implement Redis Geo (`GEOADD`, `GEORADIUS`) or PostgreSQL PostGIS (`ST_DWithin`) for $O(\log N)$ spatial indexing of millions of moving drivers.
3. **Surge Pricing:** Implement real-time dynamic pricing based on hexagonal geospatial demand/supply ratios (e.g. Uber H3 index).
4. **Driver Location Streaming:** Introduce WebSocket or MQTT for low-latency driver GPS heartbeat streaming.
5. **Security & Authentication:** Implement JWT and OAuth2 for user and driver authentication.
6. **Observability:** Add Micrometer, Prometheus, Grafana, and OpenTelemetry distributed tracing.

---

## 11. AI Usage Documentation

As requested by the assignment guidelines:
- **Assistance Provided:** AI was utilized to draft initial boilerplate structure, generate test case variations covering mathematical slab boundaries, and verify edge cases.
- **Engineering Review:** All domain models, concurrency controls (`tryAssign` atomic locking), business algorithms, REST endpoints, and integration tests were reviewed, compiled, manually verified, and executed against the local Java 18 environment.
