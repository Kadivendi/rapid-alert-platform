# Rapid Alert Platform — API Reference

**Base URL:** `http://localhost:8080/api/v1`

## Authentication

All API endpoints require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <jwt-token>
```

## Endpoints

### Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/notifications` | Create and send a notification |
| `GET` | `/notifications/{id}` | Get notification details and delivery status |
| `GET` | `/notifications` | List notifications with pagination |
| `DELETE` | `/notifications/{id}` | Cancel a pending notification |

#### Create Notification
```json
POST /api/v1/notifications
{
  "templateId": "alert-critical",
  "channels": ["telegram", "sms", "push"],
  "recipients": {
    "groupId": "zone-la-west",
    "filter": { "severity": "EXTREME" }
  },
  "payload": {
    "title": "Wildfire Evacuation Order",
    "body": "Mandatory evacuation for zones 3-7. Proceed to designated shelters.",
    "severity": "EXTREME",
    "urgency": "IMMEDIATE"
  },
  "options": {
    "dedup": true,
    "ttl": 3600,
    "priority": "HIGH"
  }
}
```

### Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health/liveness` | Kubernetes liveness probe |
| `GET` | `/health/deep` | Deep health check with component status |

### Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/metrics` | Prometheus-compatible metrics snapshot |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/dashboard/summary` | Aggregated notification analytics |

#### Query Parameters
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `hours` | int | 24 | Time window for summary |

### Webhooks

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/webhooks` | Register a webhook endpoint |
| `DELETE` | `/webhooks/{id}` | Remove a webhook registration |
| `GET` | `/webhooks/deliveries` | View webhook delivery log |

### Audit

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/audit/events` | Query audit events with filters |

#### Query Parameters
| Param | Type | Description |
|-------|------|-------------|
| `notificationId` | string | Filter by notification |
| `eventType` | enum | CREATED, SENT, DELIVERED, FAILED, RETRIED |
| `from` | ISO datetime | Start of date range |
| `to` | ISO datetime | End of date range |
| `limit` | int | Max results (default: 100) |

## Error Responses

All errors follow a standard format:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid channel: 'fax' is not a supported delivery channel",
  "timestamp": "2026-03-15T10:30:00Z",
  "path": "/api/v1/notifications"
}
```

## Rate Limits

| Channel | Rate | Burst |
|---------|------|-------|
| Telegram | 30/s | 50 |
| SMS | 100/s | 200 |
| Push | 500/s | 1000 |
| Email | 50/s | 100 |
| Mesh | 10/s | 20 |
