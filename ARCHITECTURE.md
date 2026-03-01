# Ticket Booking System — Documentación Técnica y Funcional

> Sistema de reserva de entradas tipo BookMyShow / Ticketmaster.
> Arquitectura: **Microservicios + DDD + Hexagonal (Ports & Adapters)**

---

## Tabla de Contenidos

1. [Resumen del Sistema](#1-resumen-del-sistema)
2. [Requisitos Funcionales](#2-requisitos-funcionales)
3. [Requisitos No Funcionales](#3-requisitos-no-funcionales)
4. [Estimación de Escala](#4-estimación-de-escala)
5. [Bottlenecks Identificados](#5-bottlenecks-identificados)
6. [Arquitectura General](#6-arquitectura-general)
7. [Principios de Diseño](#7-principios-de-diseño)
8. [Microservicios — Detalle](#8-microservicios--detalle)
9. [Modelo de Dominio (DDD)](#9-modelo-de-dominio-ddd)
10. [Arquitectura Hexagonal](#10-arquitectura-hexagonal)
11. [Decisiones de Diseño Clave](#11-decisiones-de-diseño-clave)
12. [API Design](#12-api-design)
13. [Flujo de Reserva End-to-End](#13-flujo-de-reserva-end-to-end)
14. [Arquitectura de Datos](#14-arquitectura-de-datos)
15. [Infraestructura y Despliegue](#15-infraestructura-y-despliegue)
16. [Estructura del Proyecto](#16-estructura-del-proyecto)
17. [Operación Local con Docker Compose](#17-operación-local-con-docker-compose)
18. [Verificación End-to-End (Checklist)](#18-verificación-end-to-end-checklist)
19. [Resumen de Diseño y Cobertura de Casos](#19-resumen-de-diseño-y-cobertura-de-casos)
20. [Escenarios de Falla y Sobrecarga](#20-escenarios-de-falla-y-sobrecarga)
21. [Escenarios Esperados Paso a Paso](#21-escenarios-esperados-paso-a-paso)

---

## 1. Resumen del Sistema

El sistema permite a usuarios comprar entradas para eventos (conciertos, partidos, teatro) en tiempo real.
Soporta hasta **100.000 usuarios concurrentes** durante picos de demanda (como el lanzamiento de entradas para un concierto masivo), garantizando que ningún asiento sea reservado dos veces.

### Stack principal

| Tecnología | Uso |
|---|---|
| Java 21 + Spring Boot 3.2 | Servicios backend |
| PostgreSQL | Datos transaccionales (bookings, usuarios) |
| MongoDB / Elasticsearch | Datos de eventos y venues (flexible, búsqueda) |
| Redis | Cache + lock distribuido de asientos (TTL 5 min) |
| Kafka | Cola de mensajes async (notificaciones, audit logs) |
| Kubernetes | Orquestación de contenedores con auto-scaling |
| Prometheus + Grafana | Monitoreo y dashboards |
| ELK Stack | Logs centralizados y búsqueda |

---

## 2. Requisitos Funcionales

### Usuarios

| # | Requisito |
|---|---|
| RF-01 | Browsear eventos disponibles con filtros por ciudad, fecha y categoría |
| RF-02 | Ver disponibilidad de asientos en tiempo real (mapa de asientos) |
| RF-03 | Reservar un asiento específico para un evento |
| RF-04 | Pagar de forma segura con tarjeta de crédito/débito |
| RF-05 | Recibir confirmación por email y SMS |
| RF-06 | Cancelar una reserva y recibir reembolso |
| RF-07 | Ver historial de reservas |

### Administradores

| # | Requisito |
|---|---|
| RF-08 | Crear y gestionar eventos (nombre, fecha, descripción) |
| RF-09 | Crear venues con su layout de asientos (filas, secciones, precios) |
| RF-10 | Publicar/cancelar eventos |
| RF-11 | Ver reportes de ventas y audit logs |

---

## 3. Requisitos No Funcionales

| Categoría | Requisito | Detalle |
|---|---|---|
| **Disponibilidad** | 99.99% uptime | Sin downtime durante picos de venta (flash sales) |
| **Latencia** | < 200ms p99 en bookings | El usuario debe saber en milisegundos si su reserva fue exitosa |
| **Escalabilidad** | Horizontal | Manejar flash sales globales sin degradación |
| **Consistencia** | Fuerte para bookings | Un asiento solo puede ser reservado por un usuario a la vez — sin double booking |
| **Auditabilidad** | Audit logs completos | Trazabilidad de cada transacción para detección de fraude |
| **Seguridad** | OAuth 2.0 + JWT | Autenticación stateless, pagos nunca expuestos al backend (tokenización) |

---

## 4. Estimación de Escala

### Usuarios

```
Total users:          5.000.000
Daily Active Users:   1.000.000
Peak concurrent:        100.000
```

### Tráfico de lectura (browse events)

```
Cada usuario browsea ~10 eventos/día
→ 1.000.000 × 10 = 10.000.000 read requests/día
→ ~116 reads/seg en promedio
→ ~1.000 reads/seg en peak
```

### Tráfico de escritura (bookings)

```
~500.000 bookings/día
→ ~6 bookings/seg en promedio
→ ~2.000 bookings/seg en peak (flash sale — ej: Taylor Swift Argentina)
```

### Conclusión de escala

- **El sistema es read-heavy**: las lecturas superan 20:1 a las escrituras en promedio.
- **El write peak es el problema crítico**: 2.000 bookings/seg en el mismo instante sobre asientos limitados genera race conditions.
- **Estrategia**: cachear lecturas en Redis, escalar horizontalmente Booking Service, usar locking distribuido.

---

## 5. Bottlenecks Identificados

### 5.1 Race Conditions en Seat Allocation
**Problema**: Múltiples usuarios intentan reservar el mismo asiento al mismo tiempo.
**Solución**: Redis `SET NX` con TTL de 5 minutos + Pessimistic Locking en DB como fallback. El `Seat` aggregate encapsula las reglas de transición de estado.

```
AVAILABLE → LOCKED  (usuario inicia reserva — Redis SET NX)
LOCKED    → BOOKED  (pago exitoso — confirma seat)
LOCKED    → AVAILABLE (TTL expiró o pago falló — release)
```

### 5.2 Database Write Pressure
**Problema**: Un spike de 2.000 bookings/seg puede saturar la DB de escrituras.
**Solución**: Múltiples réplicas de Booking Service + connection pooling (HikariCP) + particionado de la tabla de bookings por fecha.

### 5.3 Payment API Latency
**Problema**: Una llamada lenta a Stripe puede bloquear el asiento más tiempo del necesario.
**Solución**: Circuit Breaker (Resilience4j) en el `PaymentClientAdapter`. Si Stripe falla N veces seguidas, el circuito se abre y se responde inmediatamente con error, liberando el asiento.

### 5.4 Notification Backlogs
**Problema**: Enviar emails/SMS de forma sincrónica en el flujo de booking agrega latencia.
**Solución**: El `BookingApplicationService` publica un evento en Kafka después del pago. El `NotificationService` lo consume de forma asíncrona. El booking ya confirmó en milisegundos; el email puede llegar en segundos.

---

## 6. Arquitectura General

```
┌─────────────────────────────────────────────────────────────────┐
│                    Clients (Web / Mobile App)                    │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              API Gateway  (NGINX / AWS API Gateway)             │
│         Rate Limiting · Auth Filter · Request Routing           │
└──┬──────────┬──────────┬──────────┬──────────┬─────────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐ ┌───────┐ ┌──────────┐ ┌───────┐ ┌──────────────┐
│ Auth │ │ Event │ │  Seat    │ │Booking│ │   Payment    │
│ :8081│ │ Mgmt  │ │Inventory │ │ :8084 │ │   :8085      │
│      │ │ :8082 │ │  :8083   │ │       │ │              │
└──────┘ └───────┘ └──────────┘ └───┬───┘ └──────────────┘
                        │           │              │
                        │     ┌─────┘              │
                        │     │ HTTP (sync)         │ HTTP (sync)
                        ▼     ▼                    ▼
                     ┌──────────┐         ┌──────────────┐
                     │  Redis   │         │ Notification │
                     │  Cache + │         │   :8086      │
                     │  Locks   │         └──────┬───────┘
                     └──────────┘                │
                                          ┌──────▼───────┐
                                          │    Kafka     │
                                          │ Event Queue  │
                                          └──────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                         Data Layer                               │
│  ┌─────────────────────┐     ┌────────────────────────────────┐  │
│  │  PostgreSQL          │     │  MongoDB / Elasticsearch       │  │
│  │  (Bookings, Users,   │     │  (Events, Venues, Seats)       │  │
│  │   Transactions)      │     │                                │  │
│  └─────────────────────┘     └────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 7. Principios de Diseño

### 7.1 Domain-Driven Design (DDD)

El sistema está organizado en **Bounded Contexts**, uno por microservicio. Cada contexto tiene su propio lenguaje ubicuo y modelo de dominio.

| Bounded Context | Aggregate Root | Responsabilidad |
|---|---|---|
| Autenticación | `User` | Registro, login, JWT |
| Gestión de Eventos | `Event`, `Venue` | Catálogo de eventos y venues |
| Inventario de Asientos | `Seat` | Disponibilidad y locking de asientos |
| Reservas | `Booking` | Flujo de reserva y pago |
| Pagos | `Payment` | Procesamiento de pagos |
| Notificaciones | `Notification` | Email y SMS |

**Modelo Rico vs Anémico**: La lógica de negocio vive en el agregado, no en el servicio.

```java
// ❌ Modelo anémico (anti-patrón)
booking.setStatus(BookingStatus.CONFIRMED);

// ✅ Modelo rico (DDD)
booking.confirm(transactionId); // valida invariante: solo PENDING puede confirmarse
```

**Value Objects**: Clases inmutables sin identidad propia que encapsulan conceptos del dominio.

```java
// Money — garantiza que el amount siempre es positivo
Money price = Money.of(new BigDecimal("150.00"));

// Email — garantiza formato válido en el constructor
Email email = new Email("usuario@ejemplo.com"); // lanza excepción si inválido

// SeatPosition — identifica un asiento dentro de un venue
SeatPosition pos = new SeatPosition("A", 5); // Fila A, Asiento 5
```

### 7.2 Arquitectura Hexagonal (Ports & Adapters)

El dominio es el centro. No depende de ningún framework.

```
         [REST Controller]  [Kafka Consumer]        ← Adaptadores de ENTRADA
                  ↓                ↓
         [Input Port / Use Case Interface]
                  ↓
     [ Application Service ]                        ← Orquestación
                  ↓
         [Output Port Interface]
                  ↓                ↓
         [JPA Repository]  [RestTemplate Client]    ← Adaptadores de SALIDA
```

**Beneficio**: Si mañana cambiamos de JPA a MongoDB, solo cambia el adaptador. El dominio y la lógica de negocio no se tocan.

---

## 8. Microservicios — Detalle

### 8.1 auth-service (puerto 8081)

**Responsabilidad**: Registro de usuarios, login, validación de JWT.

**Bounded Context**: `Autenticación`

| Componente | Clase | Descripción |
|---|---|---|
| Aggregate | `User` | POJO. Tiene `Email` como Value Object. |
| Value Object | `Email` | Valida formato en constructor. Inmutable. |
| Input Port | `RegisterUseCase` | `register(email, password, name): Long` |
| Input Port | `LoginUseCase` | `login(email, password): String (JWT)` |
| Output Port | `UserRepositoryPort` | Contrato de persistencia |
| App Service | `AuthApplicationService` | Implementa ambos use cases |
| Adapter IN | `AuthController` | `POST /auth/register`, `POST /auth/login` |
| Adapter OUT | `UserRepositoryAdapter` | Implementa `UserRepositoryPort` vía JPA |

**Decisión de diseño**: OAuth 2.0 + JWT stateless. El token contiene `userId` y roles. Cada servicio valida el token localmente sin llamar a `auth-service`, lo que evita que sea un cuello de botella.

---

### 8.2 event-management-service (puerto 8082)

**Responsabilidad**: Catálogo de eventos y venues. Admin portal.

**Bounded Context**: `Gestión de Eventos`

| Componente | Clase | Descripción |
|---|---|---|
| Aggregate | `Event` | POJO. Referencia a `Venue` por ID (no por objeto). |
| Aggregate | `Venue` | POJO. Independiente de `Event`. |
| Input Port | `GetEventsUseCase` | `getPublishedEvents()`, `getEventsByCity(city)` |
| Input Port | `GetEventDetailsUseCase` | `getEventById(id)`, `getSeatAvailability(eventId)` |
| Input Port | `CreateEventUseCase` | `createEvent(request)` |
| Output Port | `EventRepositoryPort` | Persistencia de eventos |
| Output Port | `VenueRepositoryPort` | Persistencia de venues |
| Output Port | `SeatAvailabilityPort` | Obtiene asientos de `seat-inventory-service` |
| App Service | `EventApplicationService` | Implementa los 3 use cases |
| Adapter IN | `EventController` | Endpoints públicos de browsing |
| Adapter IN | `AdminController` | `POST /api/admin/events` |
| Adapter OUT | `SeatInventoryClientAdapter` | Llama a `:8083` via RestTemplate |

**Nota DDD**: `Event` referencia a `Venue` solo por `venueId: Long`, no por la entidad completa. Esto mantiene los aggregates independientes y evita cargas innecesarias (patrón de referencia por ID entre aggregates de distintos bounded contexts).

**Escalabilidad**: Este servicio es read-heavy (~10M req/día). En producción, `getPublishedEvents()` y `getEventById()` se cachean en Redis con TTL corto (30 segundos). Un event stale por 30 segundos es aceptable; una reserva doble no lo es.

---

### 8.3 seat-inventory-service (puerto 8083)

**Responsabilidad**: Dueño de la disponibilidad de asientos. Maneja el locking distribuido.

**Bounded Context**: `Inventario de Asientos`

| Componente | Clase | Descripción |
|---|---|---|
| Aggregate | `Seat` | POJO con `@Version` en la JPA entity para optimistic locking. |
| Value Object | `SeatPosition` | `(rowNumber, seatNumber)`. Inmutable. |
| Input Port | `LockSeatUseCase` | Lockea el asiento con TTL 5 min |
| Input Port | `ConfirmSeatUseCase` | Confirma tras pago exitoso |
| Input Port | `ReleaseSeatUseCase` | Libera (pago fallido / cancelación / TTL) |
| Input Port | `GetAvailableSeatsUseCase` | Lista asientos disponibles de un venue |
| Output Port | `SeatRepositoryPort` | Persistencia |
| Output Port | `SeatLockPort` | Lock distribuido (Redis en prod) |
| App Service | `SeatInventoryApplicationService` | Orquesta lock + DB |
| Adapter IN | `SeatController` | API interna consumida por `booking-service` |
| Adapter OUT | `SeatRepositoryAdapter` | JPA con pessimistic lock |
| Adapter OUT | `InMemorySeatLockAdapter` | `ConcurrentHashMap` en dev → Redis en prod |

**Implementación del lock** (dos capas de protección):

```
Capa 1 — Redis SET NX (lock distribuido):
  seatLock.tryLock(seatId, 5 min)  →  Redis SET seat:lock:{seatId} EX 300 NX

Capa 2 — DB Pessimistic Write Lock (fallback):
  seatRepository.findByIdWithLock(seatId)  →  SELECT ... FOR UPDATE

Capa 3 — Optimistic Locking en JPA Entity:
  @Version Long version  →  detecta escrituras concurrentes sin lock explícito
```

---

### 8.4 booking-service (puerto 8084)

**Responsabilidad**: Orquesta el flujo completo de reserva. Es el servicio más crítico.

**Bounded Context**: `Reservas`

| Componente | Clase | Descripción |
|---|---|---|
| Aggregate | `Booking` | POJO. Métodos: `confirm()`, `cancel()`, `expire()`. |
| Value Object | `Money` | `BigDecimal` + validación de no-negativo. Inmutable. |
| Input Port | `InitiateBookingUseCase` | Paso 1: lockea asiento, crea booking PENDING |
| Input Port | `ConfirmPaymentUseCase` | Paso 2: procesa pago, confirma asiento |
| Input Port | `CancelBookingUseCase` | Cancela booking y libera asiento |
| Input Port | `ReleaseExpiredBookingsUseCase` | Job periódico: expira bookings con TTL vencido |
| Input Port | `GetUserBookingsUseCase` | Historial de reservas |
| Output Port | `BookingRepositoryPort` | Persistencia de bookings |
| Output Port | `SeatLockingPort` | Comunica lock/release al seat-inventory-service |
| Output Port | `PaymentPort` | Comunica cobro al payment-service |
| Output Port | `NotificationPort` | Publica evento para notificaciones |
| App Service | `BookingApplicationService` | Implementa los 5 use cases |
| Adapter IN | `BookingController` | API pública de bookings |
| Adapter OUT | `BookingRepositoryAdapter` | JPA |
| Adapter OUT | `SeatInventoryClientAdapter` | HTTP a `:8083` |
| Adapter OUT | `PaymentClientAdapter` | HTTP a `:8085` |
| Adapter OUT | `NotificationMessageAdapter` | Publica eventos en Kafka topic `booking-events` |

**Invariantes del agregado `Booking`**:

```java
booking.confirm()  → solo si status == PENDING, si no: IllegalStateException
booking.cancel()   → no si status == EXPIRED
booking.expire()   → solo si status == PENDING
```

---

### 8.5 payment-service (puerto 8085)

**Responsabilidad**: Integrar con Stripe/Razorpay. Manejar retries e idempotency.

| Componente | Clase | Descripción |
|---|---|---|
| Aggregate | `Payment` | Resultado de un intento de pago |
| Input Port | `ProcessPaymentUseCase` | `processPayment(bookingId, token): String` |
| Output Port | `PaymentGatewayPort` | Contrato con el proveedor externo |
| App Service | `PaymentApplicationService` | Lanza excepción si el pago falla |
| Adapter IN | `PaymentController` | `POST /api/payments/process`, `POST /api/payments/webhook` |
| Adapter OUT | `StripePaymentAdapter` | Simulación con idempotencia por `bookingId` (listo para reemplazar por SDK real) |

**Idempotency Keys**: `bookingId` se envía como `Idempotency-Key` en el header de Stripe. Si la red falla y `booking-service` reintenta, Stripe devuelve el mismo `transactionId` sin cobrar dos veces.

---

### 8.6 notification-service (puerto 8086)

**Responsabilidad**: Enviar email y SMS de forma asíncrona.

| Componente | Clase | Descripción |
|---|---|---|
| Aggregate | `Notification` | Contiene bookingId, email, phone, tipo |
| Input Port | `SendNotificationUseCase` | `send(notification)` |
| Output Port | `EmailPort` | Contrato para email (AWS SES) |
| Output Port | `SmsPort` | Contrato para SMS (Twilio) |
| App Service | `NotificationApplicationService` | Decide qué enviar según el tipo |
| Adapter IN | `NotificationController` | Endpoint HTTP manual + `@KafkaListener` para consumo async |
| Adapter OUT | `SesEmailAdapter` | Stub de AWS SES |
| Adapter OUT | `TwilioSmsAdapter` | Stub de Twilio |

**Patrón async**: En producción, `booking-service` publica en Kafka topic `booking-events`. Este servicio tiene un `@KafkaListener` que consume de forma desacoplada. El booking confirma en milisegundos; la notificación llega en segundos, sin bloquear el flujo crítico.

---

## 9. Modelo de Dominio (DDD)

### Aggregates y sus invariantes

```
Booking
├── Estado: PENDING → CONFIRMED / EXPIRED / CANCELLED
├── confirm(transactionId): solo si PENDING
├── cancel(): no si EXPIRED
├── expire(): solo si PENDING
└── isExpired(): status==PENDING && now > expiresAt

Seat
├── Estado: AVAILABLE → LOCKED → BOOKED / AVAILABLE
├── lock(): solo si AVAILABLE
├── confirm(): solo si LOCKED
└── release(): no si UNAVAILABLE

Event
├── Estado: DRAFT → PUBLISHED → SOLD_OUT / CANCELLED / COMPLETED
├── publish(): solo si DRAFT
└── cancel(): no si COMPLETED

User
└── Creado con Email validado (Value Object)
```

### Value Objects

| Value Object | Servicio | Invariante |
|---|---|---|
| `Money` | booking | amount >= 0, no nulo |
| `Email` | auth | formato válido (regex), lowercase |
| `SeatPosition` | seat-inventory | rowNumber no blank, seatNumber > 0 |

### Referencia entre Bounded Contexts por ID

```java
// ✅ Correcto — Event referencia a Venue por ID
class Event {
    private Long venueId; // NO: private Venue venue
}

// ✅ Correcto — Booking referencia a User, Event, Seat por ID
class Booking {
    private Long userId;   // NO: private User user
    private Long eventId;  // NO: private Event event
    private Long seatId;   // NO: private Seat seat
}
```

Esto mantiene los bounded contexts independientes. Cada uno tiene su propia DB. No hay FK entre bases de datos.

---

## 10. Arquitectura Hexagonal

### Estructura por servicio

```
{service}/src/main/java/com/ticketbooking/{context}/
│
├── domain/                      ← 100% Java puro. Sin imports de Spring ni JPA.
│   ├── model/
│   │   ├── {Aggregate}.java     ← Lógica de negocio. Métodos de estado.
│   │   ├── {ValueObject}.java   ← Inmutable. equals/hashCode por valor.
│   │   └── enums/
│   └── port/
│       ├── in/                  ← Interfaces de casos de uso (input ports)
│       │   └── {UseCase}.java   ← Un método principal por interface
│       └── out/                 ← Interfaces de contratos externos (output ports)
│           └── {Port}.java      ← El dominio define QUÉ necesita
│
├── application/                 ← @Service. Implementa input ports. Inyecta output ports.
│   └── {Context}ApplicationService.java
│
└── infrastructure/              ← Todo lo que depende de frameworks
    ├── in/
    │   └── rest/
    │       └── {Controller}.java  ← @RestController. Traduce HTTP → input port.
    └── out/
        ├── persistence/
        │   ├── {Agg}JpaEntity.java       ← @Entity AQUÍ, no en domain/
        │   ├── {Agg}JpaRepository.java   ← Spring Data
        │   └── {Agg}RepositoryAdapter.java  ← implements output port + mapper
        ├── client/
        │   └── {Svc}ClientAdapter.java   ← RestTemplate. implements output port.
        ├── cache/
        │   └── InMemory{X}Adapter.java   ← ConcurrentHashMap en dev
        ├── gateway/
        │   └── {Provider}Adapter.java    ← Stripe, Razorpay, etc.
        ├── email/
        │   └── SesEmailAdapter.java
        └── sms/
            └── TwilioSmsAdapter.java
```

### Flujo de dependencias

```
HTTP Request
    ↓
[BookingController]          ← infrastructure/in/rest — conoce input ports
    ↓ llama
[InitiateBookingUseCase]     ← domain/port/in — interface
    ↑ implementada por
[BookingApplicationService]  ← application — @Service, conoce output ports
    ↓ llama
[SeatLockingPort]            ← domain/port/out — interface
    ↑ implementada por
[SeatInventoryClientAdapter] ← infrastructure/out/client — RestTemplate
    ↓
[seat-inventory-service]     ← otro microservicio
```

**Regla de dependencias**: Las flechas van desde afuera hacia adentro.
El dominio no importa nada de Spring, JPA, ni de los adaptadores.

### Verificación de pureza del dominio

```bash
# Este comando debe retornar vacío — el dominio no puede importar Spring ni JPA
find . -path "*/domain/*" -name "*.java" | xargs grep -l "springframework\|jakarta.persistence"
```

---

## 11. Decisiones de Diseño Clave

### 11.1 Concurrency Control: Double Booking Prevention

El problema más importante del sistema. Se usan tres capas:

**Capa 1 — Redis Distributed Lock (prevención proactiva)**
```
bookingService.initiateBooking(request)
  → seatLock.tryLock(seatId, 5 min)
  → Redis: SET seat:lock:{seatId} {bookingId} EX 300 NX
     → NX = "set only if not exists" → atómico, thread-safe
     → si ya existe → retorna false → SeatNotAvailableException
```

**Capa 2 — DB Pessimistic Lock (protección en escritura)**
```sql
SELECT * FROM seats WHERE id = ? FOR UPDATE
-- bloquea la fila hasta que termine la transacción
-- fallback si Redis no está disponible
```

**Capa 3 — @Version Optimistic Lock (detección de conflictos)**
```java
@Version Long version; // en SeatJpaEntity
// Si dos transacciones leen version=5 y ambas intentan escribir version=6,
// la segunda falla con OptimisticLockException → retry automático
```

---

### 11.2 Seat Hold Timeout (TTL-based Lock)

```
Usuario selecciona asiento → Seat: AVAILABLE → LOCKED (TTL 5 min en Redis)
                          → Booking: creado con status PENDING
                          → expiresAt = now() + 5 min

Si paga antes de 5 min:  → Seat: LOCKED → BOOKED
                          → Booking: PENDING → CONFIRMED

Si NO paga en 5 min:
  - Opción 1: Redis TTL expira → lock liberado automáticamente
  - Opción 2: Job scheduler cada 1 min:
      bookingRepo.findExpiredPendingBookings(now())
        .forEach(b → { b.expire(); seatLock.release(b.getSeatId()); })
  - Seat vuelve a AVAILABLE → otro usuario puede reservarlo
```

---

### 11.3 CQRS Pattern

Separar lecturas (queries) de escrituras (commands) permite escalar cada lado de forma independiente.

```
READS (10M/día) → event-management-service + seat-inventory-service
  → Cacheable en Redis (TTL 30 seg para eventos, 5 seg para disponibilidad)
  → Múltiples réplicas read-only de MongoDB

WRITES (500K/día, pico 2000/seg) → booking-service + seat-inventory-service
  → No cacheable
  → Requiere consistencia fuerte
  → Sharding por eventId en PostgreSQL
```

---

### 11.4 Idempotency Keys para Pagos

```
booking-service → payment-service:
  POST /api/payments/process
  Headers: Idempotency-Key: {bookingId}
  Body: { bookingId, paymentToken }

¿Por qué?
  Si la red falla y booking-service reintenta la misma llamada,
  Stripe detecta el mismo Idempotency-Key y retorna el mismo
  transactionId sin cobrar dos veces.

  Sin idempotency key: usuario cobrado 2 veces en un retry.
  Con idempotency key: garantía de exactly-once payment.
```

---

### 11.5 Circuit Breaker para Payment Service

```
booking-service llama a payment-service:

Estado CLOSED (normal):
  → Llama directo. Monitorea failures.

Estado OPEN (payment-service caído):
  → Después de N failures → circuito se abre
  → Responde inmediatamente con error
  → No espera timeout de 30 seg
  → Libera el asiento inmediatamente (releaseSeat)

Estado HALF-OPEN (recuperación):
  → Deja pasar algunas requests de prueba
  → Si exitosas → vuelve a CLOSED
```

Implementación con Resilience4j:
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
public String processPayment(Long bookingId, String token) { ... }
```

---

## 12. API Design

### auth-service

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/auth/register` | Registrar nuevo usuario |
| POST | `/auth/login` | Login → retorna JWT |

### event-management-service

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/events` | Lista eventos publicados (filtro: `?city=Buenos Aires`) |
| GET | `/api/events/{id}` | Detalle de un evento |
| GET | `/api/events/{id}/seats` | Disponibilidad de asientos en tiempo real |
| POST | `/api/admin/events` | [Admin] Crear evento |

### seat-inventory-service (API interna)

| Método | Endpoint | Descripción | Consumidor |
|---|---|---|---|
| GET | `/api/seats/venue/{venueId}` | Asientos disponibles | event-mgmt, booking |
| POST | `/api/seats/{id}/lock` | Lockear asiento (TTL 5 min) | booking |
| POST | `/api/seats/{id}/confirm` | Confirmar tras pago exitoso | booking |
| POST | `/api/seats/{id}/release` | Liberar asiento | booking |

### booking-service

| Método | Endpoint | Descripción | Request Body |
|---|---|---|---|
| POST | `/api/bookings` | Paso 1: iniciar reserva (lockea asiento) | `{userId, eventId, seatId}` |
| POST | `/api/bookings/pay` | Paso 2: confirmar pago | `{bookingId, paymentToken}` |
| DELETE | `/api/bookings/{id}` | Cancelar reserva | — |
| GET | `/api/bookings/user/{userId}` | Historial de reservas | — |

### payment-service (API interna + webhook)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/payments/process` | Procesar pago (llamado por booking-service) |
| POST | `/api/payments/webhook` | Recibir notificaciones de Stripe (firmadas con HMAC) |

---

## 13. Flujo de Reserva End-to-End

```
Usuario selecciona Asiento A-5 del Evento "Coldplay Buenos Aires"

1. GET /api/events/42/seats
   ← [event-management-service]
   → llama a seat-inventory-service: GET /api/seats/venue/7
   ← retorna lista de asientos con status AVAILABLE/LOCKED/BOOKED

2. POST /api/bookings  { userId: 1, eventId: 42, seatId: 99 }
   → [booking-service]
   → llama a seat-inventory-service: POST /api/seats/99/lock
      → Redis: SET seat:lock:99 EX 300 NX  ✓
      → DB: seat.lock() → status: AVAILABLE → LOCKED
   → crea Booking en DB: { status: PENDING, expiresAt: now+5min }
   ← retorna { bookingId: 7, expiresAt: "2024-01-01T20:05:00", totalPrice: 150.00 }

3. (Usuario ve el countdown de 5 minutos y completa los datos de pago)

4. POST /api/bookings/pay  { bookingId: 7, paymentToken: "tok_visa_..." }
   → [booking-service]
   → carga Booking 7 de DB
   → llama a payment-service: POST /api/payments/process (Idempotency-Key: 7)
      → StripePaymentAdapter.charge() → transactionId: "ch_stripe_abc123"
   → booking.confirm("ch_stripe_abc123")  [lógica en el agregado]
   → llama a seat-inventory-service: POST /api/seats/99/confirm
      → seat.confirm() → status: LOCKED → BOOKED
   → guarda Booking en DB: { status: CONFIRMED, paymentTransactionId: "ch_stripe_abc123" }
   → publica en Kafka: { type: BOOKING_CONFIRMED, bookingId: 7 }
   ← retorna { bookingId: 7, status: CONFIRMED, seatInfo: "Fila A - Asiento 5" }

5. [notification-service] consume de Kafka (async, no bloquea el paso 4)
   → envía email vía AWS SES: "Tu entrada fue confirmada!"
   → envía SMS vía Twilio: "Booking #7 confirmado"

TIEMPO TOTAL pasos 2-4: < 200ms
TIEMPO notificación (paso 5): 1-3 segundos (asíncrono)
```

### Flujo de falla de pago

```
4b. POST /api/bookings/pay  → Stripe retorna DECLINED
    → PaymentApplicationService lanza RuntimeException
    → BookingApplicationService: booking no se confirma
    → seatLock.release(99)
    → seat.release() → status: LOCKED → AVAILABLE
    → notifica al usuario: "Pago rechazado"
    ← retorna 400 Bad Request { error: "Payment failed" }
```

### Flujo de TTL expirado (sin pago en 5 min)

```
[Job periódico cada 1 minuto]
  → bookingRepo.findExpiredPendingBookings(now())
  → por cada booking expirado:
      booking.expire()              → status: PENDING → EXPIRED
      seatLock.release(seatId)      → Redis: DEL seat:lock:99
      seatRepository.save(seat)     → status: LOCKED → AVAILABLE
  → el asiento vuelve a estar disponible para otros usuarios
```

---

## 14. Arquitectura de Datos

### PostgreSQL — Datos transaccionales

Usado por: `booking-service`, `auth-service`

```sql
-- bookings (particionada por booking_time para escalar)
CREATE TABLE bookings (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT NOT NULL,
    event_id               BIGINT NOT NULL,
    seat_id                BIGINT NOT NULL,
    total_price            DECIMAL(10,2) NOT NULL,
    status                 VARCHAR(20) NOT NULL,   -- PENDING/CONFIRMED/EXPIRED/CANCELLED
    expires_at             TIMESTAMP,
    booking_time           TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP,
    payment_transaction_id VARCHAR(100)
) PARTITION BY RANGE (booking_time);

-- users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    name          VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL
);
```

### MongoDB — Datos de eventos y venues

Usado por: `event-management-service`
Ventaja: schema flexible para distintos tipos de eventos, búsqueda de texto completo via Elasticsearch.

```json
// Colección: events
{
  "_id": "64abc123",
  "name": "Coldplay Music of the Spheres",
  "description": "...",
  "startTime": "2024-03-15T21:00:00",
  "basePrice": 150.00,
  "status": "PUBLISHED",
  "venueId": "64def456",
  "tags": ["concierto", "rock", "internacional"]
}

// Colección: venues
{
  "_id": "64def456",
  "name": "Estadio Monumental",
  "city": "Buenos Aires",
  "country": "Argentina",
  "totalCapacity": 84000
}
```

### Redis — Cache y Locks

```
# Seat locks (TTL 5 minutos)
SET seat:lock:{seatId} {bookingId} EX 300 NX

# Cache de disponibilidad (TTL 5 segundos)
SET seat:availability:{venueId} {json} EX 5

# Cache de eventos (TTL 30 segundos)
SET event:{eventId} {json} EX 30
```

### Kafka — Topics

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `booking-events` | booking-service | notification-service | `{type, bookingId, userId, ...}` |
| `payment-events` | payment-service | booking-service | `{bookingId, transactionId, status}` |
| `audit-log` | todos | ELK Stack | `{service, action, userId, timestamp}` |

---

## 15. Infraestructura y Despliegue

### Kubernetes con Auto-Scaling

```yaml
# Cada servicio corre en su propio Deployment
# booking-service y seat-inventory-service tienen HPA más agresivo

# Horizontal Pod Autoscaler para booking-service
spec:
  minReplicas: 3
  maxReplicas: 50
  targetCPUUtilizationPercentage: 60
  # En un flash sale: escala de 3 a 50 pods en minutos
```

### Monitoreo

| Herramienta | Uso |
|---|---|
| **Prometheus** | Métricas: requests/seg, latencia p50/p95/p99, error rate |
| **Grafana** | Dashboards en tiempo real, alertas |
| **ELK Stack** | Logs centralizados, trazas de transacciones, fraud detection |

### Alertas críticas sugeridas

| Alerta | Threshold | Acción |
|---|---|---|
| Booking error rate | > 1% | PagerDuty — revisar payment service |
| Seat lock failures | > 5% | Revisar Redis — posible degradación |
| Latencia p99 booking | > 500ms | Escalar booking-service |
| Kafka consumer lag | > 10.000 msgs | Escalar notification-service |

---

## 16. Estructura del Proyecto

```
ticket-booking-system/
├── pom.xml                          ← Parent POM (packaging=pom)
│
├── common/                          ← Shared Kernel: DTOs inter-servicio
│   └── src/main/java/com/ticketbooking/common/
│       ├── dto/                     ← BookingRequestDTO, EventDTO, etc.
│       └── exception/               ← SeatNotAvailableException, etc.
│
├── auth-service/            :8081
│   └── .../auth/
│       ├── domain/model/            ← User, Email (Value Object)
│       ├── domain/port/in/          ← RegisterUseCase, LoginUseCase
│       ├── domain/port/out/         ← UserRepositoryPort
│       ├── application/             ← AuthApplicationService
│       └── infrastructure/
│           ├── in/rest/             ← AuthController
│           └── out/persistence/     ← UserJpaEntity, UserRepositoryAdapter
│
├── event-management-service/ :8082
│   └── .../event/
│       ├── domain/model/            ← Event, Venue, EventStatus
│       ├── domain/port/in/          ← GetEventsUseCase, CreateEventUseCase
│       ├── domain/port/out/         ← EventRepositoryPort, SeatAvailabilityPort
│       ├── application/             ← EventApplicationService
│       └── infrastructure/
│           ├── in/rest/             ← EventController, AdminController
│           ├── out/persistence/     ← JPA entities + adapters
│           └── out/client/          ← SeatInventoryClientAdapter
│
├── seat-inventory-service/   :8083
│   └── .../seat/
│       ├── domain/model/            ← Seat, SeatPosition (VO), SeatStatus
│       ├── domain/port/in/          ← LockSeatUseCase, ConfirmSeatUseCase, etc.
│       ├── domain/port/out/         ← SeatRepositoryPort, SeatLockPort
│       ├── application/             ← SeatInventoryApplicationService
│       └── infrastructure/
│           ├── in/rest/             ← SeatController
│           ├── out/persistence/     ← SeatJpaEntity + adapter
│           └── out/cache/           ← InMemorySeatLockAdapter (→ Redis en prod)
│
├── booking-service/          :8084   ← Servicio más crítico
│   └── .../booking/
│       ├── domain/model/            ← Booking (rich aggregate), Money (VO)
│       ├── domain/port/in/          ← 5 use cases
│       ├── domain/port/out/         ← BookingRepositoryPort, SeatLockingPort,
│       │                              PaymentPort, NotificationPort
│       ├── application/             ← BookingApplicationService
│       └── infrastructure/
│           ├── in/rest/             ← BookingController
│           ├── out/persistence/     ← BookingJpaEntity + adapter
│           ├── out/client/          ← SeatInventoryClientAdapter, PaymentClientAdapter
│           └── out/messaging/       ← NotificationMessageAdapter (Kafka producer)
│
├── payment-service/          :8085
│   └── .../payment/
│       ├── domain/model/            ← Payment
│       ├── domain/port/in/          ← ProcessPaymentUseCase
│       ├── domain/port/out/         ← PaymentGatewayPort
│       ├── application/             ← PaymentApplicationService
│       └── infrastructure/
│           ├── in/rest/             ← PaymentController (+ webhook)
│           └── out/gateway/         ← StripePaymentAdapter (idempotencia simulada)
│
└── notification-service/     :8086
    └── .../notification/
        ├── domain/model/            ← Notification
        ├── domain/port/in/          ← SendNotificationUseCase
        ├── domain/port/out/         ← EmailPort, SmsPort
        ├── application/             ← NotificationApplicationService
        └── infrastructure/
            ├── in/rest/             ← NotificationController (→ KafkaListener en prod)
            ├── out/email/           ← SesEmailAdapter (stub)
            └── out/sms/             ← TwilioSmsAdapter (stub)
```

---

## 17. Operación Local con Docker Compose

El proyecto incluye un `docker-compose.yml` para levantar un entorno de desarrollo integrado con:

- Microservicios: `auth`, `event-management`, `seat-inventory`, `booking`, `payment`, `notification`
- Datos y mensajería: `PostgreSQL`, `Redis`, `Kafka` (via Redpanda)
- Observabilidad: `Prometheus`, `Grafana`
- UI para mensajería: `Kafka UI`

### 17.1 Prerrequisitos

Antes de levantar el stack:

- Docker Engine instalado y corriendo.
- Docker Compose v2 disponible (`docker compose version`).
- Puertos libres: `3000`, `5432`, `6379`, `8081-8087`, `9090`, `9092`.
- Ejecutar comandos desde la raíz del repo:
  - `/home/avaca/Documentos/ticket-booking-system`

Comandos de validación rápida:

```bash
docker --version
docker compose version
docker info > /dev/null && echo "Docker OK"
```

### 17.2 Servicios y puertos

| Componente | Puerto host | URL |
|---|---|---|
| auth-service | 8081 | `http://localhost:8081` |
| event-management-service | 8082 | `http://localhost:8082` |
| seat-inventory-service | 8083 | `http://localhost:8083` |
| booking-service | 8084 | `http://localhost:8084` |
| payment-service | 8085 | `http://localhost:8085` |
| notification-service | 8086 | `http://localhost:8086` |
| Kafka UI | 8087 | `http://localhost:8087` |
| Prometheus | 9090 | `http://localhost:9090` |
| Grafana | 3000 | `http://localhost:3000` (`admin/admin`) |
| PostgreSQL | 5432 | `localhost:5432` |
| MongoDB | 27017 | `localhost:27017` |
| Redis | 6379 | `localhost:6379` |
| Kafka broker | 9092 | `localhost:9092` |

### 17.3 Primer arranque (limpio)

```bash
cd /home/avaca/Documentos/ticket-booking-system
sudo docker compose down -v
sudo docker compose up -d --build
sudo docker compose ps
```

> Si tu usuario ya tiene permisos sobre Docker socket, podés ejecutar los mismos comandos sin `sudo`.

### 17.4 Arranque recomendado por fases

Para diagnóstico más simple, conviene levantar por capas:

```bash
# 1) Infra base
docker compose up -d postgres mongodb redis kafka kafka-ui

# 2) Servicios core
docker compose up -d auth-service seat-inventory-service event-management-service payment-service notification-service booking-service

# 3) Observabilidad
docker compose up -d prometheus grafana
```

Si falla una fase, revisar logs de ese grupo antes de continuar.

### 17.5 Comandos operativos frecuentes

```bash
# Ver estado
docker compose ps

# Ver logs en vivo de todo el stack
docker compose logs -f

# Ver logs de un servicio puntual
docker compose logs -f event-management-service

# Ver últimos 200 logs de booking
docker compose logs --tail=200 booking-service

# Reiniciar un servicio
docker compose restart booking-service

# Rebuild de un solo servicio (sin tocar el resto)
docker compose up -d --build booking-service

# Forzar recreación de contenedor de un servicio
docker compose up -d --force-recreate booking-service

# Apagar stack (sin borrar volúmenes)
docker compose down
```

### 17.6 FAQ operativa (reinicios y cambios de código)

#### A) Quiero reiniciar TODO el stack

Tenés dos opciones según necesidad:

```bash
# Reinicio "rápido" (mantiene contenedores y datos)
docker compose restart

# Reinicio "limpio" de contenedores (mantiene datos)
docker compose down
docker compose up -d
```

Si además querés reconstruir imágenes (por cambios de código), usar:

```bash
docker compose down
docker compose up -d --build
```

Si querés empezar desde cero (incluye borrar datos):

```bash
docker compose down -v --remove-orphans
docker compose up -d --build
```

#### B) Quiero reiniciar un solo servicio

```bash
# Solo reinicia el contenedor actual
docker compose restart booking-service

# Recrea solo ese contenedor
docker compose up -d --force-recreate booking-service
```

#### C) Si cambio código de un módulo, ¿tengo que buildear de nuevo?

Sí. En este setup los servicios corren como imágenes Docker, sin hot reload de código fuente montado por volumen.
Por eso, cambios en Java/clases de un módulo requieren rebuild de imagen de ese servicio.

```bash
# Rebuild solo del módulo modificado
docker compose up -d --build booking-service

# Verificar logs después del rebuild
docker compose logs -f --tail=200 booking-service
```

Regla práctica:

- Cambiaste código del módulo `X` -> `docker compose up -d --build X`.
- Cambiaste solo configuración runtime (algunas variables/env) -> `restart` o `up -d` puede alcanzar.
- Cambiaste dependencias/base image/Dockerfile -> conviene `--build`.

### 17.7 Verificación de salud del entorno

1. Estado de contenedores:

```bash
docker compose ps
```

2. Health endpoints de apps:

```bash
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost:8082/actuator/health
curl -fsS http://localhost:8083/actuator/health
curl -fsS http://localhost:8084/actuator/health
curl -fsS http://localhost:8085/actuator/health
curl -fsS http://localhost:8086/actuator/health
```

3. Infra crítica:

```bash
# PostgreSQL
docker compose exec postgres pg_isready -U ticketbook -d ticketbook

# Redis
docker compose exec redis redis-cli ping

# Kafka (Redpanda)
docker compose exec kafka rpk cluster health
```

### 17.8 Base de datos local

El init script `infra/postgres/init/01-create-databases.sql` crea:

- `authdb`
- `eventdb`
- `seatdb`
- `bookingdb`

Las credenciales por defecto del contenedor Postgres son:

- user: `ticketbook`
- password: `ticketbook`

Conexión rápida por consola:

```bash
docker compose exec postgres psql -U ticketbook -d bookingdb
```

### 17.9 Datos y volúmenes (persistencia)

Los datos viven en volúmenes Docker (`postgres-data`, `redis-data`, `kafka-data`, etc.).

- `docker compose down`: apaga, mantiene datos.
- `docker compose down -v`: apaga y elimina datos persistidos.

Reset total del entorno:

```bash
docker compose down -v --remove-orphans
docker compose up -d --build
```

### 17.10 Métricas y observabilidad

Todos los microservicios exponen:

- `GET /actuator/health`
- `GET /actuator/prometheus`

Prometheus scrapea esos endpoints usando `monitoring/prometheus/prometheus.yml`.
Grafana viene con datasource de Prometheus autoprovisionado desde `monitoring/grafana/provisioning/datasources/datasource.yml`.

### 17.11 Troubleshooting específico de Compose

- Error `permission denied ... /var/run/docker.sock`
  - Ejecutar con `sudo` o corregir permisos del grupo `docker`.
- Error `port is already allocated`
  - Identificar proceso/stack ocupando el puerto y liberar.
  - Ejemplo: `lsof -i :8084`.
- Un servicio queda en `Restarting`
  - Revisar `docker compose logs --tail=200 <service>`.
  - Verificar variables de entorno y dependencia disponible.
- Cambios de código no impactan en contenedor
  - Rebuild del servicio: `docker compose up -d --build <service>`.
- Fallos por residuos de estado
  - Reset con `docker compose down -v --remove-orphans`.

### 17.12 Estado actual de integraciones

El entorno de infraestructura está disponible en Docker y ya incluye integraciones clave del flujo crítico:

- `NotificationMessageAdapter` publica en Kafka (`booking-events`)
- `notification-service` consume con `@KafkaListener`
- `event-management-service` soporta persistencia Mongo para catálogo (switch por propiedad)
- `seat-inventory-service` soporta lock Redis real vía `RedisSeatLockAdapter`
- `StripePaymentAdapter` implementa idempotencia por `bookingId`
- `payment-service` valida firma de webhook (`Stripe-Signature`)

Aún pendiente para ambiente productivo real:

- reemplazar adapters simulados por SDKs reales (Stripe/SES/Twilio),
- agregar circuit breaker/retries/bulkheads en llamadas síncronas críticas,
- consolidar tests de carga end-to-end con infraestructura real.

---

## 18. Verificación End-to-End (Checklist)

### 18.1 Health checks

```bash
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8082/actuator/health
curl -i http://localhost:8083/actuator/health
curl -i http://localhost:8084/actuator/health
curl -i http://localhost:8085/actuator/health
curl -i http://localhost:8086/actuator/health
```

### 18.2 Probar API básica de eventos

```bash
# Listado inicial (puede estar vacío)
curl -i http://localhost:8082/api/events

# Crear evento demo
curl -X POST http://localhost:8082/api/admin/events \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Coldplay Live",
    "description":"Demo event",
    "startTime":"2026-03-10T21:00:00",
    "basePrice":150.00
  }'

# Verificar creación
curl -i http://localhost:8082/api/events
```

### 18.3 Probar auth básico

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@demo.com","password":"123456","name":"Test User"}'
```

### 18.4 Troubleshooting rápido

- Error `permission denied ... /var/run/docker.sock`
  - Ejecutar con `sudo` o reingresar sesión con grupo `docker`.
- Error `no configuration file provided: not found`
  - Ejecutar comandos desde la raíz del repo (`ticket-booking-system`).
- Error HTTP 500 por `Name for argument ... not specified`
  - Ya corregido en controllers con `@PathVariable("...")` y `@RequestParam(name="...")`.
- Si un servicio no responde:
  - `sudo docker compose ps`
  - `sudo docker compose logs --tail=200 <service>`

---

## 19. Resumen de Diseño y Cobertura de Casos

Esta sección resume el **qué**, el **por qué** y los **riesgos cubiertos** del diseño actual, en formato de repaso rápido.

### 19.1 ¿Qué construimos?

Se construyó una plataforma de ticketing en tiempo real, dividida en microservicios por capacidad de negocio:

- `auth-service`: identidad, registro y login con JWT.
- `event-management-service`: catálogo de eventos y venues.
- `seat-inventory-service`: disponibilidad y locking de asientos.
- `booking-service`: orquestación de reserva y confirmación.
- `payment-service`: procesamiento de pago e idempotencia.
- `notification-service`: envío async de email/SMS.

Arquitecturalmente, cada servicio sigue **DDD + Hexagonal**:

- Dominio: agregados, value objects e invariantes.
- Puertos: contratos de entrada/salida (`port/in`, `port/out`).
- Adaptadores: REST, persistencia, clients, mensajería.

### 19.2 ¿Por qué se diseñó así?

1. **El riesgo principal es el double booking**, no el catálogo.
   Por eso el diseño prioriza consistencia fuerte en reservas y asientos.
2. **El sistema es read-heavy**, pero los picos de escritura son críticos.
   Separar servicios permite escalar lecturas y escrituras de forma independiente.
3. **Se buscó desacoplar dominio de frameworks**.
   Así, un cambio tecnológico (JPA, proveedor de pago, mensajería) no obliga a rehacer reglas de negocio.
4. **Se minimizó latencia en el camino crítico**.
   Notificaciones son asíncronas para que la confirmación de compra no espere email/SMS.
5. **Se diseñó para fallas parciales**.
   Circuit breaker, timeouts e idempotencia reducen impacto cuando proveedores externos fallan.

### 19.3 ¿De qué casos nos cubrimos?

1. **Muchos usuarios reservando el mismo asiento al mismo tiempo**  
   Cobertura en tres capas:
   - Lock distribuido (Redis `SET NX` con TTL).
   - Lock pesimista en base (`SELECT ... FOR UPDATE`).
   - Lock optimista con `@Version` para detectar conflictos.
2. **Usuario no paga dentro de la ventana de hold**  
   TTL de 5 minutos + job de expiración; el asiento vuelve a `AVAILABLE`.
3. **Reintentos por red inestable durante el pago**  
   `Idempotency-Key` por `bookingId` evita doble cobro.
4. **Proveedor de pago lento/caído**  
   Circuit breaker falla rápido y permite liberar asiento sin degradación prolongada.
5. **Picos de notificaciones**  
   Kafka desacopla confirmación de compra de envío email/SMS.
6. **Operación y trazabilidad**  
   Métricas, logs y health checks para detectar degradación antes de impactar usuarios.

### 19.4 Relación con `EventJpaEntity` (ejemplo concreto)

Archivo: `event-management-service/src/main/java/com/ticketbooking/event/infrastructure/out/persistence/EventJpaEntity.java`

Esta clase ilustra una decisión clave de Hexagonal:

- `EventJpaEntity` vive en `infrastructure/out/persistence` porque representa **persistencia**.
- El agregado de dominio `Event` vive en `domain/model` porque representa **reglas de negocio**.
- El dominio no conoce JPA/Spring; infraestructura traduce entre modelo de dominio y entidad de base.

Beneficio directo:

- Se puede cambiar la tecnología de persistencia sin romper los casos de uso.
- El dominio se testea en aislamiento, con menos acoplamiento técnico.
- Se mantiene clara la separación entre modelo de negocio y modelo de almacenamiento.

### 19.5 Estado actual y próximos pasos naturales

La arquitectura objetivo ya está definida, y el entorno Docker está preparado. Según el estado actual:

- `NotificationMessageAdapter` ya publica eventos de booking en Kafka.
- `notification-service` ya consume esos eventos con `@KafkaListener`.
- `seat-inventory-service` ya tiene `RedisSeatLockAdapter` (seleccionable por config).
- `payment-service` ya aplica idempotencia por `bookingId` en `StripePaymentAdapter`.
- `payment-service` ya valida firma de webhook por HMAC (`Stripe-Signature`).
- `event-management-service` ya puede ejecutar repositorio de eventos/venues sobre MongoDB.

Próximos pasos recomendados para cerrar el gap de producción:

1. Reemplazar adapter de pago simulado por integración Stripe SDK completa (PaymentIntent + errores reales).
2. Conectar envío real de email/SMS (SES/Twilio) con estrategia de retries y dead-letter.
3. Endurecer resiliencia de llamadas sync (timeouts/circuit breaker/bulkhead).
4. Escalar suite de stress tests E2E para validar comportamiento bajo picos sostenidos.

---

## 20. Escenarios de Falla y Sobrecarga

Esta sección explica qué ocurre en escenarios reales de operación.
Formato usado en cada caso:

- **Qué ve el usuario**
- **Qué pasa internamente**
- **Estado actual (repo) vs objetivo (producción)**

### 20.1 Se cae `event-management-service`

- **Qué ve el usuario**: falla en browse/listado de eventos (`5xx` o timeout).
- **Qué pasa internamente**: el servicio de catálogo no responde; no se pueden resolver `GET /api/events` ni `GET /api/events/{id}`.
- **Estado actual (repo)**: no hay fallback en otro servicio; la funcionalidad de catálogo queda indisponible.
- **Objetivo (producción)**: múltiples réplicas + cache de lectura para reducir impacto de caída de una instancia.

### 20.2 Se cae `booking-service`

- **Qué ve el usuario**: no puede iniciar ni confirmar reservas.
- **Qué pasa internamente**: se interrumpe la orquestación de lock + pago + confirmación.
- **Estado actual (repo)**: caída total del flujo de compra.
- **Objetivo (producción)**: varias réplicas, auto-scaling y health checks para reemplazo rápido de instancias.

### 20.3 Se cae `seat-inventory-service`

- **Qué ve el usuario**:
  - no ve disponibilidad en tiempo real;
  - no puede lockear/confirmar/liberar asientos.
- **Qué pasa internamente**: `event-management-service` y `booking-service` dependen de sus endpoints internos.
- **Estado actual (repo)**: flujo de reserva queda bloqueado (no hay servicio alternativo de locking).
- **Objetivo (producción)**: escalar horizontalmente + lock distribuido robusto (Redis real) + timeouts para fail-fast.

### 20.4 Se cae `payment-service` o responde muy lento

- **Qué ve el usuario**: errores al pagar o rechazo inmediato si circuito abierto.
- **Qué pasa internamente**: `booking-service` no puede confirmar pago; la reserva no pasa a `CONFIRMED`.
- **Estado actual (repo)**: existe diseño de circuit breaker en documentación; implementación completa depende del estado del adapter.
- **Objetivo (producción)**: circuit breaker + timeout + release de asiento para evitar locks largos y mala UX.

### 20.5 Se cae Kafka

- **Qué ve el usuario**:
  - normalmente puede completar compra;
  - puede demorarse/perderse confirmación por email/SMS (según integración activa).
- **Qué pasa internamente**: falla publicación/consumo de eventos async (`booking-events`, etc.).
- **Estado actual (repo)**: booking sigue confirmando por flujo sync y notificaciones quedan degradadas hasta recuperar broker.
- **Objetivo (producción)**: compra no bloqueada (flujo sync), pero notificaciones y procesos async quedan degradados hasta recuperar broker.

### 20.6 Se cae Redis

- **Qué ve el usuario**: más rechazos por contención o mayor latencia en reservas concurrentes.
- **Qué pasa internamente**: se pierde lock distribuido/caché; el sistema recae más en locking de DB.
- **Estado actual (repo)**: existe lock Redis real configurable y fallback in-memory para entornos sin Redis.
- **Objetivo (producción)**: fallback a lock pesimista de DB, con menor throughput pero manteniendo consistencia.

### 20.7 Se cae PostgreSQL

- **Qué ve el usuario**: errores masivos en operaciones transaccionales (auth/bookings/eventos según servicio).
- **Qué pasa internamente**: no se puede persistir/leer estado crítico; las APIs devuelven error.
- **Estado actual (repo)**: sin base no hay operación de negocio consistente.
- **Objetivo (producción)**: alta disponibilidad de DB, backups, réplicas y runbooks de failover.

### 20.8 Llegan más requests de las soportadas (sobrecarga general)

- **Qué ve el usuario**: mayor latencia, timeouts y aumento de errores en picos.
- **Qué pasa internamente**: saturación de CPU, pools de conexión, colas HTTP y/o dependencia externa.
- **Estado actual (repo)**: en local no hay auto-scaling; se observa degradación directa.
- **Objetivo (producción)**: HPA, rate limiting en gateway, cache para reads y tuning de conexiones.

### 20.9 Hotspot extremo: miles de usuarios por el mismo asiento

- **Qué ve el usuario**:
  - uno confirma;
  - la mayoría recibe “seat not available”.
- **Qué pasa internamente**: compiten por el mismo recurso; lock evita que dos transacciones confirmen el mismo asiento.
- **Estado actual (repo)**: protegido por diseño de locking en capas (con diferencias entre stub/dev y objetivo prod).
- **Objetivo (producción)**: lock distribuido Redis + DB lock + optimistic versioning para tolerar picos sin double booking.

### 20.10 Timeouts en cascada entre servicios

- **Qué ve el usuario**: request lenta o error aunque el servicio inicial esté “up”.
- **Qué pasa internamente**: un timeout en dependencia (ej. pagos) consume hilos/recursos y degrada todo el flujo.
- **Estado actual (repo)**: riesgo típico de arquitectura sync entre microservicios.
- **Objetivo (producción)**: timeouts estrictos, circuit breakers, retries controlados e aislamiento de bulkheads.

### 20.11 Qué se prioriza cuando todo falla a la vez

Orden recomendado de prioridad operativa:

1. **Consistencia de reserva**: nunca double booking.
2. **Disponibilidad del core de compra**: lock + pago + confirmación.
3. **Degradación elegante**: notificaciones/reportes pueden quedar async o diferidos.
4. **Recuperación observable**: métricas/alertas/logs para volver a estado sano rápido.

---

## 21. Escenarios Esperados Paso a Paso

Esta sección describe el comportamiento **esperado** del sistema, paso a paso, para los flujos más importantes.

### 21.1 Compra exitosa (flujo nominal)

1. Usuario consulta asientos de un evento (`GET /api/events/{id}/seats`).
2. `event-management-service` consulta disponibilidad en `seat-inventory-service`.
3. Usuario inicia reserva (`POST /api/bookings` con `userId`, `eventId`, `seatId`).
4. `booking-service` solicita lock del asiento en `seat-inventory-service`.
5. `seat-inventory-service` marca asiento `LOCKED` y responde OK.
6. `booking-service` crea `Booking` en estado `PENDING` con `expiresAt` (+5 min).
7. Usuario paga (`POST /api/bookings/pay`).
8. `booking-service` llama a `payment-service` con idempotency key.
9. Si pago OK, `booking-service` confirma booking (`PENDING -> CONFIRMED`).
10. `booking-service` pide confirmar asiento (`LOCKED -> BOOKED`) en `seat-inventory-service`.
11. `booking-service` devuelve respuesta exitosa al cliente.
12. En paralelo, se publica evento de notificación para email/SMS (async).

### 21.2 Pago rechazado

1. Booking ya está `PENDING` y asiento `LOCKED`.
2. Usuario intenta pagar.
3. `payment-service` responde rechazo/error de cobro.
4. `booking-service` no confirma booking.
5. `booking-service` libera asiento en `seat-inventory-service` (`LOCKED -> AVAILABLE`).
6. Cliente recibe error de pago; puede reintentar mientras no expire la reserva.

### 21.3 Expiración por TTL (usuario no paga a tiempo)

1. Usuario crea reserva y queda `PENDING` con `expiresAt`.
2. No se recibe pago antes del vencimiento.
3. Job periódico detecta bookings `PENDING` vencidos.
4. Cada booking vencido pasa a `EXPIRED`.
5. Se libera lock del asiento.
6. Asiento vuelve a `AVAILABLE` para otro usuario.

### 21.4 Cancelación iniciada por usuario

1. Usuario solicita cancelar booking (`DELETE /api/bookings/{id}`).
2. `booking-service` valida estado del booking (según reglas del agregado).
3. Si la transición es válida, booking pasa a `CANCELLED`.
4. Si el asiento estaba retenido o reservado según política, se libera en inventario.
5. Se registra evento/trace de cancelación para auditoría.

### 21.5 `payment-service` caído durante pago (degradación esperada)

1. Usuario llega al paso de pago.
2. `booking-service` intenta llamar a `payment-service`.
3. Si hay timeout/falla repetida, se activa política de resiliencia (circuit breaker en objetivo prod).
4. Se responde error rápido al cliente (fail-fast), en lugar de esperar timeouts largos.
5. El asiento se libera para evitar locks innecesarios.
6. Usuario puede reintentar más tarde.

### 21.6 Kafka caído durante confirmaciones

1. Compra y confirmación de booking ocurren por flujo síncrono.
2. Publicación de evento async falla (notificaciones).
3. Usuario ve confirmación de compra igualmente.
4. Email/SMS puede demorarse o no enviarse hasta recuperar broker.
5. Al recuperar Kafka, consumidores retoman procesamiento pendiente según estrategia de reintento.

### 21.7 Sobrecarga de requests (pico de tráfico)

1. Aumenta bruscamente el tráfico (browse o booking).
2. Suben latencias y presión sobre CPU/DB/pools.
3. Se prioriza proteger consistencia de reservas sobre throughput bruto.
4. Requests excedentes pueden recibir timeout, `429` o error controlado según política de entrada.
5. En producción, auto-scaling y cache amortiguan el pico; en local se degrada más rápido.

### 21.8 Dos usuarios intentan el mismo asiento al mismo tiempo

1. Usuario A y B intentan reservar `seatId` idéntico.
2. Solo uno obtiene lock del asiento.
3. El otro recibe “seat not available” (o rechazo equivalente).
4. Si el ganador paga, asiento termina `BOOKED`.
5. Si no paga y expira TTL, asiento vuelve a `AVAILABLE`.
6. En ningún caso ambos deben terminar con confirmación del mismo asiento.

### 21.9 Servicio caído y recuperación (ciclo esperado)

1. Un servicio falla y su health check queda `DOWN`.
2. Dependientes empiezan a fallar en las llamadas que lo necesitan.
3. Operación detecta incidente por alertas/logs.
4. Se reinicia/recrea el servicio afectado (`docker compose restart <service>` o `up -d --build <service>`).
5. Health vuelve a `UP`.
6. Tráfico retorna gradualmente a normalidad.
7. Se valida backlog y consistencia post-incidente (bookings, asientos, eventos async).

*Documentación generada para el proyecto ticket-booking-system — Arquitectura DDD + Hexagonal + Microservicios*
