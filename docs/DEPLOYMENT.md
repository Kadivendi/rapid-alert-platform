# Deployment Guide

## Production Architecture

```
                    ┌──────────────────┐
                    │   Load Balancer   │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
     ┌────────▼───┐  ┌──────▼──────┐  ┌───▼────────┐
     │ API Gateway │  │ API Gateway │  │ API Gateway │
     │  (replica)  │  │  (replica)  │  │  (replica)  │
     └────────┬───┘  └──────┬──────┘  └───┬────────┘
              └──────────────┼──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │     Apache Kafka Cluster     │
              │    (3 brokers, RF=3)         │
              └──────────────┬──────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
┌────────▼───────┐  ┌───────▼────────┐  ┌──────▼────────┐
│ Notification   │  │ Sender         │  │ Rebalancer    │
│ Service (x3)   │  │ Service (x5)   │  │ Service (x2)  │
└────────┬───────┘  └───────┬────────┘  └───────────────┘
         │                   │
┌────────▼───────┐  ┌───────▼────────┐
│  PostgreSQL    │  │     Redis      │
│  (primary +    │  │   (Sentinel)   │
│   replica)     │  │                │
└────────────────┘  └────────────────┘
```

## Docker Compose (Development)

```bash
docker compose -f docker/docker-compose.dev.yml up -d
```

## Kubernetes (Production)

```bash
# Apply namespace and secrets
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml

# Deploy infrastructure
kubectl apply -f k8s/kafka/
kubectl apply -f k8s/postgres/

# Deploy application services
kubectl apply -f k8s/services/

# Verify
kubectl get pods -n rapid-alert
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | — | Kafka broker addresses |
| `DATABASE_URL` | Yes | — | PostgreSQL connection string |
| `REDIS_URL` | No | `localhost:6379` | Redis connection URL |
| `JWT_SECRET` | Yes | — | JWT signing secret |
| `TELEGRAM_BOT_TOKEN` | No | — | Telegram Bot API token |
| `SMS_PROVIDER_KEY` | No | — | SMS gateway API key |
| `PUSH_FIREBASE_KEY` | No | — | Firebase Cloud Messaging key |

## Monitoring

- **Prometheus**: Scrape `/api/v1/metrics` every 15s
- **Grafana**: Import dashboard from `monitoring/grafana-dashboard.json`
- **Alerting**: Configure alerts for delivery failure rate > 5%
