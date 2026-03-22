# Contributing to Rapid Alert Platform

Thank you for considering contributing to the Rapid Alert Platform. This document provides guidelines for contributing to the project.

## Development Setup

### Prerequisites
- JDK 17+
- Docker and Docker Compose
- Gradle 8.x (included via wrapper)

### Local Development
```bash
# Start infrastructure services
docker compose -f docker/docker-compose.dev.yml up -d

# Build all modules
./gradlew build

# Run notification service
./gradlew :notification-service:bootRun

# Run tests
./gradlew test
```

## Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use 4-space indentation
- Max line length: 120 characters
- Always add Javadoc to public classes and methods

## Commit Conventions

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(scope): add new feature
fix(scope): fix bug description
refactor(scope): code improvement
test(scope): add or update tests
docs(scope): documentation changes
chore(scope): build/tooling changes
```

### Scopes
- `notification` — notification-service module
- `gateway` — api-gateway module
- `kafka` — Kafka configuration and consumers
- `security` — authentication and authorization
- `health` — health check and monitoring
- `webhook` — webhook dispatcher
- `dedup` — deduplication engine
- `audit` — audit trail

## Pull Request Process

1. Create a feature branch from `main`
2. Write tests for new functionality
3. Ensure all tests pass: `./gradlew test`
4. Update documentation if needed
5. Submit PR with clear description of changes

## Architecture Decisions

Major architecture decisions are documented in `docs/`. When proposing significant changes, please open an issue first to discuss the approach.

## Reporting Issues

Use GitHub Issues with the following labels:
- `bug` — Something isn't working
- `enhancement` — Feature request
- `documentation` — Documentation improvements
- `performance` — Performance-related issues
