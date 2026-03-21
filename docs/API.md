# Rapid Alert Platform — API Reference

**Base URL:** `http://localhost:8080/api/v1`

All endpoints are routed through the API Gateway. The Gateway centrally validates
JWTs against the Security Service (with a 30s in-memory cache) and injects
`X-Client-Id` into every downstream request.

## Authentication

```
POST /api/v1/auth/register        — Register new client credentials
POST /api/v1/auth/authenticate    — Exchange credentials for a JWT
GET  /api/v1/auth/validate        — Validate a JWT (internal — used by the gateway)
```

Once you have a token, send it on every other request:

```
Authorization: Bearer <jwt-token>
```

The route `/api/v1/auth/login` is aliased to `/authenticate` for the documented login flow.

## Endpoints

### Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/notifications/{templateId}` | Trigger a fan-out for the given template |
| `POST` | `/notifications/{id}/sent` | Mark a notification as successfully delivered |
| `POST` | `/notifications/{id}/error` | Mark a notification as failed |
| `POST` | `/notifications/{id}/corrupt` | Mark a notification payload as undeliverable |
| `POST` | `/notifications/{id}/resending` | Increment retry counter and re-queue |
| `GET`  | `/notifications/` | List of `NotificationKafka` items the rebalancer should re-publish |

Trigger a fan-out:

```http
POST /api/v1/notifications/42
clientId: 7
```

This looks up template 42, partitions its recipient list across the live
`notification-service` instances (count from Eureka), and writes one
`RecipientListKafka` message per partition to the `recipient-list-splitter` topic.

### Recipients

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`    | `/recipients` | List recipients (paginated) |
| `POST`   | `/recipients` | Register a new recipient |
| `PUT`    | `/recipients/{id}` | Update recipient details |
| `DELETE` | `/recipients/{id}` | Remove a recipient |

### Templates

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/templates` | List templates |
| `POST` | `/templates` | Create a template |
| `PUT`  | `/templates/{id}` | Update a template |
| `GET`  | `/templates/{id}/history` | View Debezium-backed change history |

### Files (bulk recipient import)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/files/` | Upload `.xlsx` to register recipients in bulk |
| `GET`  | `/files/` | Download an `.xlsx` of the caller's recipients |

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health/liveness` | Kubernetes liveness probe |
| `GET` | `/health/deep` | Component-by-component health snapshot |

### Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/metrics` | Snapshot of every counter / gauge tracked by `NotificationMetricsService` |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/dashboard/summary?hours=24` | Aggregated counts of audit events and channel mix |

### Webhooks

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST`   | `/webhooks` | Register a webhook (`url`, `secret`, `eventTypes`) |
| `DELETE` | `/webhooks/{id}` | Remove a webhook |
| `GET`    | `/webhooks/deliveries?limit=100` | Recent delivery attempts |

Lifecycle events emitted by `NotificationService`:
`notification.created`, `notification.queued`, `notification.sent`,
`notification.failed`, `triage.received`, `mesh.dispatched`. Subscribe with
`"eventTypes": ["*"]` to receive everything.

### Audit

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/audit/events` | Query the audit trail |
| `GET` | `/audit/counts` | Group counts by event type |

Query parameters for `/audit/events`:

| Param | Type | Description |
|-------|------|-------------|
| `notificationId` | string | Filter by notification id |
| `eventType` | enum | `CREATED`, `QUEUED`, `SENT`, `DELIVERED`, `FAILED`, `RETRIED`, `EXPIRED`, `DEDUPLICATED` |
| `from` | ISO instant | Lower bound on timestamp |
| `to` | ISO instant | Upper bound on timestamp |
| `limit` | int | Max results (default 100) |

## Cross-project integration

| Path | Producer | Consumer | Topic |
|------|----------|----------|-------|
| Triage events | `disaster-triage-engine` `/api/v1/triage` | `notification-service` (`TriageEventConsumer`) | `rapid-alert.triage-events` |
| Mesh failover | `notification-service` (`MeshFailoverDispatcher`) | `resilient-mesh-gateway` `/api/mesh/broadcast` | n/a (HTTP) |

## Error responses

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid channel: 'fax' is not a supported delivery channel",
  "timestamp": "2026-03-15T10:30:00Z",
  "path": "/api/v1/notifications"
}
```

## Rate limits (per-channel token bucket, applied in `sender`)

| Channel | Rate | Burst |
|---------|------|-------|
| Telegram | 30/s | 50 |
| SMS | 100/s | 200 |
| Push | 500/s | 1000 |
| Email | 50/s | 100 |
| Mesh | 10/s | 20 |
