# 🚨 Wikipedia Edit War Detection System

A real-time streaming application that detects edit wars on Wikipedia using **Apache Kafka**, **Spring Boot**, and **reactive programming**.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-green)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.9-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Tests](https://img.shields.io/badge/Tests-Passing-brightgreen)
![TDD](https://img.shields.io/badge/TDD-100%25%20Coverage-success)

## 🎯 What It Does

Monitors the [Wikimedia EventStreams API](https://stream.wikimedia.org/v2/stream/recentchange) in real-time and detects patterns indicating **edit wars** - situations where multiple users repeatedly revert each other's changes on the same article.

**Real Detection:** Successfully detected edit war on **Frederick Trump** page with 2 conflicting editors! ✅

## 🏗️ Architecture
```
Wikimedia API → Kafka Producer → Kafka Topic → Kafka Consumer → Edit War Detection → PostgreSQL → REST API
```

### Components

1. **kafka-producer-api**: Streams real-time Wikipedia edits to Kafka
2. **kafka-consumer-api**: Consumes events, detects edit wars, exposes REST API

### Technologies

- **Spring Boot 3.5.6** - Application framework
- **Apache Kafka 3.9** - Event streaming platform
- **Spring WebFlux** - Reactive programming & Server-Sent Events
- **PostgreSQL 15** - Database persistence
- **Spring Data JPA** - ORM with Hibernate
- **Spring Data JPA Repositories** - Data access layer
- **JUnit 5 & Mockito** - Testing with TDD approach
- **Maven** - Build tool
- **Project Lombok** - Boilerplate reduction

## 🔍 Edit War Detection Algorithm

### Criteria

An edit war is detected when:
- ✅ **5+ edits** on the same article within 1 hour
- ✅ **2-3 distinct human editors** (bots excluded)
- ✅ **Main namespace only** (articles, not talk pages)
- ✅ **50%+ conflict ratio** (reverts or opposing changes)

### Conflict Types

1. **Pure Reverts**: Edit returns article to a previous length
2. **Opposing Edits**: One user adds content, another removes it

### Why Real Alerts Are Rare

Edit wars occur in only ~0.01% of all edits. Most Wikipedia activity consists of collaborative editing, making our successful detection of real edit wars particularly significant!

## 🚀 Getting Started

### Prerequisites

- **Java 21+**
- **Apache Kafka 3.9+** (KRaft mode)
- **PostgreSQL 15+**
- **Maven 3.8+**

### Database Setup
```bash
# Create database and user
psql -U postgres
CREATE DATABASE editwars_detection;
CREATE USER editwar_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE editwars_detection TO editwar_user;
\q
```

### Kafka Setup
```bash
# Download and extract Kafka
wget https://downloads.apache.org/kafka/3.9.0/kafka_2.13-3.9.0.tgz
tar -xzf kafka_2.13-3.9.0.tgz
cd kafka_2.13-3.9.0

# Generate cluster ID and format storage (first time only)
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties

# Start Kafka
bin/kafka-server-start.sh config/kraft/server.properties
```

### Application Setup
```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/springboot-kafka-realtime.git
cd springboot-kafka-realtime

# Build project
./mvnw clean install

# Configure database connection
# Edit kafka-consumer-api/src/main/resources/application.properties
# Update: spring.datasource.url, username, password

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

### Example Usage
```bash
# Health check
curl http://localhost:8081/api/health | jq

# Get statistics
curl http://localhost:8081/api/stats | jq

# Get all alerts
curl http://localhost:8081/api/alerts | jq

# Search for specific topic
curl "http://localhost:8081/api/alerts/search?q=trump" | jq

# Simulate test data
curl -X POST http://localhost:8081/api/test/simulate-edit-war | jq
```

## 🧪 Testing

**Test-Driven Development (TDD) approach** with 100% coverage of critical paths:
```bash
# Run all tests
./mvnw test

# Run specific test suites
./mvnw test -Dtest=AlertServiceTest
./mvnw test -Dtest=AlertControllerTest
./mvnw test -Dtest=EditWarDetectionServiceTest
```

### Test Coverage

- ✅ Unit tests for services, repositories, mappers
- ✅ Integration tests with H2 in-memory database
- ✅ REST API tests with WebTestClient
- ✅ Mock-based isolation testing

## 📊 Project Structure
```
springboot-kafka-realtime/
├── kafka-producer-api/              # Wikimedia → Kafka producer
│   ├── ApiRealTimeChangesProducer   # SSE client
│   ├── ApiRealTimeChangesHandler    # Event handler
│   └── KafkaTopicConfig             # Topic configuration
├── kafka-consumer-api/              # Kafka → Processing → API
│   ├── entity/                      # Domain models
│   │   ├── EditWarAlert
│   │   ├── WikimediaEditEvent
│   │   └── PageEditWindow
│   ├── persistence/                 # Database layer
│   │   ├── entity/                  # JPA entities
│   │   ├── repository/              # Spring Data repos
│   │   └── mapper/                  # Domain ↔ Entity mappers
│   ├── service/                     # Business logic
│   │   ├── EditWarDetectionService
│   │   ├── AlertService
│   │   └── WikimediaEventParser
│   └── controller/                  # REST endpoints
│       ├── AlertController
│       └── TestDataController
└── README.md
```

## 🎯 Key Features

- ✅ **Real-time processing** - Processes Wikipedia edits as they happen
- ✅ **Pattern recognition** - Sophisticated conflict detection algorithm
- ✅ **Reactive architecture** - Non-blocking I/O with Spring WebFlux
- ✅ **Database persistence** - PostgreSQL with JPA/Hibernate
- ✅ **RESTful API** - Comprehensive endpoints with pagination
- ✅ **Test-driven** - Extensive test coverage
- ✅ **Production-ready** - Error handling, logging, monitoring

## 🏆 Achievements

- 🎯 Successfully detected real edit war on **Frederick Trump** Wikipedia page
- ✅ Processed 1000+ Wikipedia edits in real-time
- ✅ 100% test coverage on critical business logic
- ✅ Clean architecture with separation of concerns
- ✅ Scalable event-driven design



## 📝 Technical Highlights

### Design Patterns Used
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
- Reactive Programming
- RESTful API design
- Database indexing strategies

## 🐛 Troubleshooting

### No events appearing?
- Check Kafka is running: `jps | grep Kafka`
- Verify topic exists: `kafka-topics.sh --list --bootstrap-server localhost:9092`
- Check producer connection logs

### No alerts appearing?
- This is normal! Real edit wars are rare (~0.01% of edits)
- Use test endpoints: `POST /api/test/simulate-edit-war`

### Database connection issues?
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check credentials in `application.properties`

## 📄 License

MIT License - See LICENSE file for details

## 👤 Author

Eugene Paitoo

[LinkedIn](https://www.linkedin.com/in/eugene-paitoo/)

## 🙏 Acknowledgments

- Wikimedia Foundation for providing the EventStreams API
- Spring/Apache Kafka communities
- Built with Test-Driven Development methodology

---

**⭐ Star this repo if you find it useful!**

---

*Note: This is a learning/portfolio project demonstrating real-time stream processing, event-driven architecture, and production-grade Java development practices.*
