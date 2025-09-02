<div align="center">

# ⚡ Rapid Alert Platform

**A cloud-native, event-driven microservices platform for delivering real-time mass emergency notifications at scale — the core delivery backbone of a four-project resilient alert ecosystem.**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.0-6DB33F?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-7.3.2-231F20?style=for-the-badge&logo=apache-kafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Alpine-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge)](https://github.com/Kadivendi/rapid-alert-platform/actions)

<br/>

> Rapid Alert Platform enables emergency management organizations to broadcast targeted notifications to millions of recipients simultaneously — across Telegram, email, and SMS — with built-in reliability, JWT-secured access, automatic retry, and horizontal scalability via Apache Kafka.

[Overview](#-overview) · [Ecosystem](#-ecosystem) · [Architecture](#-architecture) · [Services](#-services) · [API Docs](#-api-documentation) · [Getting Started](#-getting-started) · [Contributing](#-contributing)

</div>

---

## 📌 Overview

Rapid Alert Platform is a production-ready distributed system built with the microservices pattern, designed to solve a single hard problem: **how do you reliably notify a massive number of people, fast, without losing a single message — even when parts of the system fail?**

The answer is a pipeline of independently deployable services, each with a clear responsibility, communicating asynchronously via Apache Kafka. Whether you're alerting 100 users or 10,000,000 — the platform scales horizontally by spinning up additional service instances, which are automatically discovered and load-balanced via Eureka.

This platform is the **delivery backbone** of a broader emergency alert ecosystem. It receives pre-classified alerts from the AI triage layer, dispatches them over available channels, and falls back to offline mesh delivery when standard infrastructure is unavailable.

---

## 🌐 Ecosystem

This repository is **Module 1** of a four-part interconnected emergency communication platform. Each module is independently deployable but designed to work together as a cohesive system:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      EMERGENCY ALERT ECOSYSTEM                               │
├──────────────────────┬─────────────────────────┬───────────────────────────┤
│  disaster-triage-    │  cap-ipaws-bridge        │  resilient-mesh-gateway   │
│  engine              │                          │                           │
│  Python · FastAPI    │  Python · FEMA IPAWS     │  Flutter · BLE · LoRa     │
│  PyTorch · Kafka     │  CAP 1.2 · Wagtail       │  WiFi Direct · AES-256    │
│                      │                          │                           │
│  AI severity         │  Federal alert ingest    │  Offline mesh delivery    │
│  classification &    │  & CAP composition       │  when towers go down      │
│  escalation forecast │                          │                           │
└──────────┬───────────┴────────────┬─────────────┴────────────┬──────────────┘
           │                        │                           │
           └────────────────────────▼───────────────────────────┘
                                    │
                    ┌───────────────▼──────────────────┐
                    │       rapid-alert-platform         │ ← YOU ARE HERE
                    │                                    │
                    │  Core notification dispatch engine │
                    │  Kafka · Spring Boot · Eureka      │
                    │  JWT · PostgreSQL · Testcontainers │
                    └────────────────────────────────────┘
```

| Module | Repo | Role |
|---|---|---|
| **Module 1** | [rapid-alert-platform](https://github.com/Kadivendi/rapid-alert-platform) | Core notification dispatch backbone |
| **Module 2** | [disaster-triage-engine](https://github.com/Kadivendi/disaster-triage-engine) | AI/ML severity classification + escalation forecasting |
| **Module 3** | [resilient-mesh-gateway](https://github.com/Kadivendi/resilient-mesh-gateway) | Offline BLE/WiFi/LoRa mesh alert delivery |
| **Module 4** | [cap-ipaws-bridge](https://github.com/Kadivendi/cap-ipaws-bridge) | FEMA IPAWS-OPEN + CAP 1.2 federal integration |

### How They Connect

1. `disaster-triage-engine` ingests real-time NOAA/USGS sensor data, classifies each event's severity, and publishes triage results to the `rapid-alert.triage-events` Kafka topic consumed by **this service**.
2. `cap-ipaws-bridge` polls FEMA's IPAWS-OPEN API, validates CAP 1.2 alerts, and routes them here for multi-channel dispatch.
3. When standard delivery fails (delivery rate < 80%), this platform signals `resilient-mesh-gateway` to initiate offline BLE/LoRa mesh broadcast for affected zones.

---

## ✨ Key Features

| Feature | Status | Details |
|---|:---:|---|
| 📨 **Telegram Notifications** | ✅ Live | Real-time dispatch via Telegram Bot API with delivery tracking |
| 📧 **Email Notifications** | ✅ Live | SMTP multi-provider with template rendering |
| 📱 **Push Notifications** | ✅ Live | Firebase FCM integration |
| 📩 **SMS Alerts** | ✅ Live | Twilio integration for SMS broadcast |
| 📂 **Bulk Recipient Import** | ✅ Live | Upload `.xlsx` to register thousands of recipients instantly |
| 🗒️ **Notification Templates** | ✅ Live | Reusable templates with variable substitution via CDC |
| 🔁 **Automatic Retry** | ✅ Live | Failed messages auto-requeued via Rebalancer service |
| 📍 **Geolocation Targeting** | ✅ Live | Filter recipients by geographic region polygon |
| 🔐 **JWT Authentication** | ✅ Live | Stateless auth enforced at the API Gateway layer |
| 🔗 **URL Shortening** | ✅ Live | Embedded links auto-shortened in notification payloads |
| 📊 **Notification History** | ✅ Live | Full delivery audit trail per recipient and template |
| 🌐 **Mesh Failover Signal** | ✅ Live | Triggers `resilient-mesh-gateway` when delivery rate drops |
| 🤖 **Triage Integration** | ✅ Live | Consumes `rapid-alert.triage-events` from disaster-triage-engine |

---

## 🏗️ Architecture

The platform follows an **event-driven microservices architecture** orchestrated through Apache Kafka. Every service is independently deployable, registered with Eureka for dynamic routing, and communicates asynchronously to achieve fault tolerance and horizontal scalability.

```
                            ┌─────────────────────────────┐
                            │  External Alert Sources      │
                            │  • disaster-triage-engine    │
                            │  • cap-ipaws-bridge          │
                            └──────────────┬──────────────┘
                                           │ Kafka: rapid-alert.triage-events
                                           ▼
Client ──────► API Gateway (port 8080) ──► JWT Validation (Security Service)
                     │
          ┌──────────┴────────────────────────────────┐
          │                                            │
          ▼                                            ▼
  Recipient Service                           Notification Service
  (CRUD, bulk import,                         (orchestration, state machine,
   Kafka partitioning)                         at-least-once delivery guarantee)
          │                                            │
          │  Kafka: notification.created               │
          └───────────────────────────────────────────►│
                                                        │
                              ┌─────────────────────────┤
                              │                         │
                              ▼                         ▼
                       Sender Service           Rebalancer Service
                    (Telegram/Email/SMS)    (RESENDING state recovery)
                              │
                   delivery failure?
                              │
                              ▼
                  ┌──────────────────────┐
                  │ resilient-mesh-       │
                  │ gateway (fallback)    │
                  └──────────────────────┘
```

### Scalability Model

When a notification request targets 1,000,000 recipients:

1. **Partition** — Recipient Service queries Eureka for running instance count (e.g., 100). Splits recipient list into 100 equal batches of 10,000.
2. **Distribute** — Each batch published to Kafka; each running Notification Service instance picks up one batch in parallel.
3. **Deliver** — Concurrent dispatch to Sender Service via Telegram Bot API / SMTP / SMS provider.
4. **Recover** — Failures flagged as `RESENDING`. Rebalancer sweeps periodically and re-publishes, guaranteeing **at-least-once delivery**.
5. **Failover** — Delivery rate monitor computes rolling average. If < 80%, mesh bridge signal sent to `resilient-mesh-gateway`.

---

## 🧩 Services

| Service | Port | Responsibility |
|---|:---:|---|
| `api-gateway` | `8080` | Central entry point — routes requests, enforces JWT, aggregates Swagger UI |
| `discovery-server` | `8761` | Eureka service registry for dynamic discovery and load balancing |
| `security-service` | `8081` | Issues and validates JWT tokens; manages client credentials |
| `recipient-service` | `8082` | CRUD for recipients; Kafka-based bulk partitioned processing |
| `template-service` | `8083` | Reusable notification templates with Debezium CDC support |
| `notification-service` | `8084` | Core orchestration; tracks full delivery state machine per notification |
| `sender` | `8085` | Dispatches Telegram/email messages; marks failures as `RESENDING` |
| `rebalancer` | `8086` | Scheduled recovery job — re-queues stuck `RESENDING` notifications |
| `file-service` | `8087` | Parses `.xlsx` uploads; registers recipients in bulk via REST |
| `url-shortener` | `8088` | Shortens embedded URLs in notification payloads |
| `telegram-bot-server` | `8089` | Interactive Telegram bot for recipient self-service and responses |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.1, Spring Cloud 2022.0.3 |
| **Messaging** | Apache Kafka (Confluent 7.3.2) |
| **API Gateway** | Spring Cloud Gateway |
| **Service Discovery** | Netflix Eureka |
| **Databases** | PostgreSQL per-service (isolated schemas) |
| **Auth** | JWT (JJWT 0.11.5), Spring Security |
| **API Docs** | SpringDoc OpenAPI 3 / Swagger UI |
| **Object Mapping** | MapStruct |
| **Boilerplate** | Lombok |
| **Testing** | JUnit 5, Testcontainers, AssertJ, WireMock |
| **CDC** | Debezium (template-service change data capture) |
| **Containerization** | Docker, Docker Compose |
| **Package Namespace** | `com.rapidalert.*` |

---

## 🚀 Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 20+
- [Docker Compose](https://docs.docker.com/compose/) v3.8+
- Java 17+ (for local development only)

### Run with Docker Compose

```bash
# 1. Clone the repository
git clone https://github.com/Kadivendi/rapid-alert-platform.git
cd rapid-alert-platform

# 2. Start all services
docker compose up -d

# 3. Verify all containers are healthy
docker compose ps
```

| Service | URL |
|---|---|
| Swagger UI (all endpoints) | http://localhost:8080/webjars/swagger-ui/index.html |
| Eureka Dashboard | http://localhost:8761 |

### Local Development (single service)

```bash
# Run a specific service locally against a Docker-hosted Kafka/Postgres
cd notification-service
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Run Tests

```bash
# All tests (requires Docker for Testcontainers)
./gradlew test

# Single service
cd recipient-service && ./gradlew test
```

Integration tests use **Testcontainers** to spin up real PostgreSQL and Kafka instances — no mocking of infrastructure.

---

## 📡 API Documentation

All endpoints are accessible via the **centralized Swagger UI** at the API Gateway — no need to check individual services:

```
http://localhost:8080/webjars/swagger-ui/index.html
```

<details>
<summary><strong>🔐 Security — Authentication & Token Management</strong></summary>

- `POST /api/v1/auth/authenticate` — Authenticate and receive a JWT (aliased as `/login`)
- `POST /api/v1/auth/register` — Register new client credentials
- `GET  /api/v1/auth/validate` — Validate a JWT (used internally by the API Gateway, cached 30s)

</details>

<details>
<summary><strong>👥 Recipients — Register, Update & Manage</strong></summary>

- `GET /api/v1/recipients` — List all recipients (paginated)
- `POST /api/v1/recipients` — Register a new recipient with geolocation
- `PUT /api/v1/recipients/{id}` — Update recipient contact details
- `DELETE /api/v1/recipients/{id}` — Remove a recipient
- `POST /api/v1/files/` — Bulk import recipients via `.xlsx`

</details>

<details>
<summary><strong>📋 Templates — Create & Manage Notification Templates</strong></summary>

- `GET /api/v1/templates` — List all templates
- `POST /api/v1/templates` — Create a new notification template
- `PUT /api/v1/templates/{id}` — Update template content
- `GET /api/v1/templates/{id}/history` — View edit history (CDC-backed)

</details>

<details>
<summary><strong>🔔 Notifications — Send & Track</strong></summary>

- `POST /api/v1/notifications/{templateId}` — Trigger a notification campaign for the given template
- `POST /api/v1/notifications/{id}/sent` — Mark a notification as delivered
- `POST /api/v1/notifications/{id}/error` — Mark as failed
- `POST /api/v1/notifications/{id}/resending` — Re-queue for delivery
- `GET  /api/v1/notifications/` — Rebalancer view of stuck notifications

</details>

<details>
<summary><strong>📊 Observability — Audit, Dashboard, Metrics, Webhooks</strong></summary>

- `GET  /api/v1/audit/events` — Query the lifecycle audit trail (filters: notificationId, eventType, from/to, limit)
- `GET  /api/v1/audit/counts` — Counts by event type
- `GET  /api/v1/dashboard/summary?hours=24` — Aggregated analytics
- `GET  /api/v1/metrics` — Prometheus-compatible snapshot
- `POST /api/v1/webhooks` — Register a webhook (`url`, `secret`, `eventTypes`)
- `DELETE /api/v1/webhooks/{id}` — Remove a webhook
- `GET  /api/v1/webhooks/deliveries?limit=100` — Recent delivery attempts
- `GET  /api/v1/health/liveness` — Liveness probe
- `GET  /api/v1/health/deep` — Component-by-component status

Lifecycle events emitted to webhooks: `notification.created`, `notification.queued`,
`notification.sent`, `notification.failed`, `triage.received`, `mesh.dispatched`.

</details>

---

## 🔐 Security Model

All requests require a valid **JWT Bearer token** issued by the Security Service:

```
1. Client  →  POST /security/login  →  Security Service
2. Security Service returns signed JWT (RS256)
3. Client  →  ANY request + Authorization: Bearer <token>  →  API Gateway
4. API Gateway validates JWT with Security Service (cached with TTL)
5. On success: client ID injected as X-Client-Id request header
6. Downstream service reads header for request scoping — never re-validates
```

No downstream service validates tokens directly. **All authentication is centralized at the gateway.** This single-validation-point model reduces latency and eliminates token validation logic duplication across services.

---

## 🧪 Testing Strategy

| Test Type | Framework | Coverage |
|---|---|---|
| **Unit tests** | JUnit 5 + Mockito | Service layer, mappers, validators |
| **Integration tests** | Testcontainers + AssertJ | Full HTTP request → database round-trips |
| **Contract tests** | WireMock | Inter-service Feign client stubs |
| **Kafka tests** | Embedded Kafka | Producer/consumer lifecycle verification |

Integration tests use **real Docker containers** (PostgreSQL + Kafka) via Testcontainers — no in-memory fakes that mask production behavior.

---

## 🤝 Contributing

```bash
# Fork, then:
git checkout -b feat/your-feature-name
git commit -m "feat(scope): describe your change"
git push origin feat/your-feature-name
# Open a Pull Request
```

Follow [Conventional Commits](https://www.conventionalcommits.org/) for all commit messages.

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">
  <sub>Module 1 of the <a href="https://github.com/Kadivendi">Rapid Alert Platform ecosystem</a> — resilient emergency notification infrastructure built for the moments when it matters most.</sub>
</div>
