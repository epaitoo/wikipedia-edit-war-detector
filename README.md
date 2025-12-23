# 🚨 Wikipedia Edit War Detection System

A real-time streaming application that detects edit wars on Wikipedia using **Apache Kafka**, **Spring Boot**, **React**, and **Docker**.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![React](https://img.shields.io/badge/React-18-61DAFB)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![Tests](https://img.shields.io/badge/Tests-Passing-brightgreen)

## 🎯 What It Does

Monitors the [Wikimedia EventStreams API](https://stream.wikimedia.org/v2/stream/recentchange) in real-time and detects patterns indicating **edit wars** - situations where multiple users repeatedly revert each other's changes on the same article.

**Real Detection:** Successfully detected edit wars on pages like **Frederick Trump**, **Hans van Manen**, and more! ✅

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Wikimedia API  │────▶│  Kafka Producer │────▶│   Apache Kafka  │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  React Frontend │◀────│    REST API     │◀────│  Kafka Consumer │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │   PostgreSQL    │
                                                └─────────────────┘
```

### Components

| Component | Description |
|-----------|-------------|
| **kafka-producer-api** | Streams real-time Wikipedia edits to Kafka |
| **kafka-consumer-api** | Consumes events, detects edit wars, exposes REST API |
| **React Frontend** | Dashboard displaying real-time alerts (separate repo) |

### Technologies

- **Spring Boot 3.5.6** - Application framework
- **Apache Kafka (KRaft)** - Event streaming (no ZooKeeper required)
- **Spring WebFlux** - Reactive programming & Server-Sent Events
- **PostgreSQL 15** - Database persistence
- **Spring Data JPA** - ORM with Hibernate
- **React 18 + TypeScript** - Frontend dashboard
- **Docker & Docker Compose** - Containerization
- **JUnit 5 & Mockito** - Testing with TDD approach

## 🚀 Quick Start with Docker

The fastest way to run the entire stack:

### Prerequisites

- **Docker** and **Docker Compose** installed
- **Git**

### 1. Clone and Configure

```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/springboot-kafka-realtime.git
cd springboot-kafka-realtime

# Create environment file
cp .env.example .env

# (Optional) Edit .env to change database credentials
```

### 2. Start Everything

```bash
# Build and start all services
docker-compose up --build

# Or run in background
docker-compose up --build -d
```

This starts:
- ✅ **PostgreSQL** - Database with schema auto-initialized
- ✅ **Apache Kafka** - Message broker (KRaft mode)
- ✅ **Producer** - Streams Wikipedia events to Kafka
- ✅ **Consumer** - Detects edit wars, serves REST API on port 8081

### 3. Verify It's Working

```bash
# Check all containers are running
docker-compose ps

# View logs
docker-compose logs -f

# Test the API
curl http://localhost:8081/api/health | jq
curl http://localhost:8081/api/stats | jq
curl http://localhost:8081/api/alerts | jq
```

### 4. Stop Everything

```bash
docker-compose down

# To also remove the database volume (fresh start)
docker-compose down -v
```

## 🖥️ Local Development (Without Docker)

If you prefer running services locally:

### Prerequisites

- **Java 21+**
- **Apache Kafka 3.8+** (KRaft mode)
- **PostgreSQL 15+**
- **Maven 3.8+**

### Database Setup

```bash
# Create database and user
psql -U postgres
CREATE DATABASE editwars_detection;
CREATE USER editwar_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE editwars_detection TO editwar_user;
\c editwars_detection
# Run the schema migration
\i kafka-consumer-api/src/main/resources/db/migration/V1__init_schema.sql
\q
```

### Kafka Setup (KRaft Mode)

```bash
# Download and extract Kafka
wget https://downloads.apache.org/kafka/3.8.0/kafka_2.13-3.8.0.tgz
tar -xzf kafka_2.13-3.8.0.tgz
cd kafka_2.13-3.8.0

# Generate cluster ID and format storage (first time only)
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties

# Start Kafka
bin/kafka-server-start.sh config/kraft/server.properties
```

### Application Setup

```bash
# Build project
./mvnw clean install

# Start Consumer (in one terminal)
cd kafka-consumer-api
../mvnw spring-boot:run

# Start Producer (in another terminal)
cd kafka-producer-api  
../mvnw spring-boot:run
```

## 📡 REST API Endpoints

Base URL: `http://localhost:8081/api`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/stats` | System statistics |
| GET | `/alerts` | Get all alerts (paginated) |
| GET | `/alerts/{id}` | Get specific alert |
| GET | `/alerts/search?q={keyword}` | Search by page title |
| GET | `/alerts/status/{status}` | Filter by status |
| GET | `/alerts/severity/{level}` | Filter by severity |
| GET | `/alerts/recent` | Recent active alerts |
| POST | `/test/simulate-edit-war` | Simulate test data |

### Example Responses

```bash
# Get statistics
curl http://localhost:8081/api/stats | jq
{
  "totalAlerts": 12,
  "activeAlerts": 12,
  "resolvedAlerts": 0
}

# Search for alerts
curl "http://localhost:8081/api/alerts/search?q=trump" | jq

# Get high severity alerts
curl http://localhost:8081/api/alerts/severity/HIGH | jq
```

## 🔍 Edit War Detection Algorithm

### Criteria

An edit war is detected when:
- ✅ **5+ edits** on the same article within 1 hour
- ✅ **2-3 distinct human editors** (bots excluded)
- ✅ **Main namespace only** (articles, not talk pages)
- ✅ **50%+ conflict ratio** (reverts or opposing changes)

### Conflict Types

| Type | Description |
|------|-------------|
| **Pure Reverts** | Edit returns article to a previous length |
| **Opposing Edits** | One user adds content, another removes it |

### Severity Levels

| Level | Score | Description |
|-------|-------|-------------|
| CRITICAL | ≥0.8 | Intense, rapid conflict |
| HIGH | ≥0.6 | Significant edit war |
| MEDIUM | ≥0.4 | Moderate conflict |
| LOW | <0.4 | Minor disagreement |

## 🧪 Testing

**Test-Driven Development (TDD) approach** with comprehensive coverage:

```bash
# Run all tests
./mvnw test

# Run specific test suites
./mvnw test -Dtest=AlertServiceTest
./mvnw test -Dtest=AlertControllerTest
./mvnw test -Dtest=EditWarDetectionServiceTest
./mvnw test -Dtest=PageEditWindowTest
```

### Test Coverage

- ✅ Unit tests for services, repositories, mappers
- ✅ Integration tests with H2 in-memory database
- ✅ REST API tests with WebTestClient
- ✅ Edit war detection algorithm tests

## 📊 Project Structure

```
springboot-kafka-realtime/
├── docker-compose.yml           # Container orchestration
├── .env.example                 # Environment template
├── kafka-producer-api/          # Wikimedia → Kafka
│   ├── Dockerfile
│   ├── src/main/java/.../
│   │   ├── ApiRealTimeChangesProducer.java
│   │   ├── ApiRealTimeChangesHandler.java
│   │   └── KafkaTopicConfig.java
│   └── src/main/resources/
│       ├── application.properties
│       └── application-docker.properties
├── kafka-consumer-api/          # Kafka → Detection → API
│   ├── Dockerfile
│   ├── src/main/java/.../
│   │   ├── controller/          # REST endpoints
│   │   ├── service/             # Business logic
│   │   ├── entity/              # Domain models
│   │   └── persistence/         # Database layer
│   └── src/main/resources/
│       ├── application.properties
│       ├── application-docker.properties
│       └── db/migration/        # SQL schemas
└── README.md
```

## 🐳 Docker Configuration

### Services

| Service | Image | Port | Description |
|---------|-------|------|-------------|
| postgres | postgres:15-alpine | 5433:5432 | Database |
| kafka | apache/kafka:latest | 9092:9092 | Message broker |
| producer | Custom build | - | Wikimedia streamer |
| consumer | Custom build | 8081:8081 | API server |

### Environment Variables

Create a `.env` file (see `.env.example`):

```env
POSTGRES_DB=editwars_detection
POSTGRES_USER=editwar_user
POSTGRES_PASSWORD=your_secure_password
```

### Useful Commands

```bash
# View logs for specific service
docker-compose logs -f consumer

# Rebuild single service
docker-compose up --build consumer

# Access PostgreSQL
docker exec -it editwars-postgres psql -U editwar_user -d editwars_detection

# Check Kafka topics
docker exec -it editwars-kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

## 🎯 Key Features

- ✅ **Real-time processing** - Processes Wikipedia edits as they happen
- ✅ **Pattern recognition** - Sophisticated conflict detection algorithm
- ✅ **Reactive architecture** - Non-blocking I/O with Spring WebFlux
- ✅ **Database persistence** - PostgreSQL with JPA/Hibernate
- ✅ **RESTful API** - Comprehensive endpoints with pagination
- ✅ **Containerized** - One-command deployment with Docker Compose
- ✅ **Test-driven** - Extensive test coverage
- ✅ **Production-ready** - Error handling, logging, health checks

## 🐛 Troubleshooting

### Port already in use?

```bash
# PostgreSQL conflict (if running locally)
# Change docker-compose.yml: "5433:5432" instead of "5432:5432"

# Or stop local PostgreSQL
sudo systemctl stop postgresql
```

### No events appearing?

```bash
# Check producer logs
docker-compose logs -f producer

# Verify Kafka is receiving messages
docker exec -it editwars-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic wikimedia-stream-api \
  --from-beginning
```

### Database schema issues?

```bash
# Reset database (removes all data)
docker-compose down -v
docker-compose up --build
```

### No alerts appearing?

This is normal! Real edit wars are rare (~0.01% of edits). Use test endpoints:
```bash
curl -X POST http://localhost:8081/api/test/simulate-edit-war | jq
```

## 📝 Technical Highlights

### Design Patterns
- Repository Pattern (data access)
- Mapper Pattern (DTO conversion)
- Observer Pattern (event-driven)
- Builder Pattern (object construction)

### Architecture Principles
- Clean Architecture / Layered Architecture
- Separation of Concerns
- Dependency Inversion
- Single Responsibility

### Best Practices
- Test-Driven Development (TDD)
- Spring Profiles for environment configuration
- Docker multi-stage builds
- Health checks for container orchestration

## 📄 License

MIT License - See LICENSE file for details

## 👤 Author

**Eugene Paitoo**

[LinkedIn](https://www.linkedin.com/in/eugene-paitoo/)

---

**⭐ Star this repo if you find it useful!**

*This project demonstrates real-time stream processing, event-driven architecture, containerization, and production-grade Java development practices.*