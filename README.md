# Wikipedia Edit War Detection System

A real-time streaming application that detects edit wars on Wikipedia using Apache Kafka, Spring Boot, and reactive programming.

## 🎯 What It Does

Monitors the [Wikimedia EventStreams API](https://stream.wikimedia.org/v2/stream/recentchange) and detects patterns indicating edit wars - situations where multiple users repeatedly revert each other's changes on the same article.

## 🏗️ Architecture
```
Wikimedia API → Kafka Producer → Kafka Topic → Kafka Consumer → Edit War Detection → Alerts
```

### Components

1. **kafka-producer-api**: Streams real-time Wikipedia edits to Kafka
2. **kafka-consumer-api**: Consumes events, detects edit wars, streams to frontend

### Technologies

- **Spring Boot 3.5.6** - Application framework
- **Apache Kafka** - Event streaming platform
- **Spring WebFlux** - Reactive programming & Server-Sent Events (SSE)
- **LaunchDarkly EventSource** - SSE client for Wikimedia API
- **Project Lombok** - Boilerplate reduction

## 🔍 Edit War Detection Algorithm

### Criteria

An edit war is detected when:
- ✅ **5+ edits** on the same article within 1 hour
- ✅ **2-3 distinct human editors** (bots excluded)
- ✅ **Main namespace only** (articles, not talk pages/files)
- ✅ **50%+ conflict ratio** (reverts or opposing changes)

### Conflict Detection

The system identifies two types of conflicts:

1. **Pure Reverts**: Edit returns article to a previous length
2. **Opposing Edits**: One user adds content, another removes it

### Why Real Alerts Are Rare

Edit wars are surprisingly uncommon (~0.01% of all edits). Most Wikipedia activity consists of:
- Constructive additions by collaborating editors
- Bot maintenance (formatting, categorization)
- Non-conflicting improvements

**This rarity actually validates Wikipedia's community health!**

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Apache Kafka 4.1+ (KRaft mode supported)
- Maven 3.8+

### Installation

1. **Clone the repository**
```bash
git clone <https://github.com/epaitoo/springboot-kafka-realtime>
cd springboot-kafka-realtime
```

2. **Start Kafka**
```bash
# If using KRaft mode (recommended)
bin/kafka-server-start.sh config/kraft/server.properties

# Or with Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
```

3. **Build the project**
```bash
mvn clean install
```

4. **Start the Producer**
```bash
cd kafka-producer-api
mvn spring-boot:run
```

5. **Start the Consumer** (in new terminal)
```bash
cd kafka-consumer-api
mvn spring-boot:run
```

6. **View the stream**

Open browser to: `http://localhost:8081/stream`

## 🧪 Testing & Validation

Since real edit wars are rare, the system provides multiple validation methods:

### Unit Tests
```bash
cd kafka-consumer-api
mvn test
```

Tests validate:
- ✅ Detection logic with reverting patterns
- ✅ Filtering of bots and non-article namespaces
- ✅ Handling of edge cases (single user, too many users)
- ✅ Time window expiration

### Test Endpoints

**Simulate classic reverting war:**
```bash
curl -X POST http://localhost:8081/api/test/simulate-edit-war
```

**Simulate opposing edits:**
```bash
curl -X POST http://localhost:8081/api/test/simulate-opposing-edits
```

**Get detection statistics:**
```bash
curl http://localhost:8081/api/test/stats
```

**Expected output:**
```json
{
  "success": true,
  "scenario": "Classic Reverting War",
  "page": "Donald_Trump_1729848923",
  "users": ["Alice", "Bob"],
  "totalEdits": 5,
  "alertTriggered": true,
  "severity": "MEDIUM",
  "conflictRatio": "80%"
}
```

## 📊 Monitoring

### Consumer Logs

Watch for these indicators:

**Normal operation:**
```
Processing edit on page: Article_Name by user: Username
Added edit to page en.wikipedia.org:Article_Name: 3 edits in window
```

**Edit war detected:**
```
🚨🚨🚨 EDIT WAR DETECTED 🚨🚨🚨
Page: Article_Name
Users: [Alice, Bob]
Severity: 0.75 (HIGH)
Edits: 6 (83% conflict)
```

### Producer Logs
```
✅ Connection to Wikimedia OPENED!
📨 Received event: {"title":"..."}
```

## 🎓 Project Value

This project demonstrates:

- **Event-driven architecture** with Kafka
- **Reactive programming** with Spring WebFlux
- **Real-time data processing** from external APIs
- **Pattern recognition algorithms** for conflict detection
- **Production-ready testing** strategies
- **Separation of concerns** (production vs. test data)

## 📁 Project Structure
```
springboot-kafka-realtime/
├── kafka-producer-api/
│   ├── ApiRealTimeChangesProducer.java    # Wikimedia SSE client
│   ├── ApiRealTimeChangesHandler.java     # Event handler
│   └── KafkaTopicConfig.java              # Topic configuration
├── kafka-consumer-api/
│   ├── entity/
│   │   ├── WikimediaEditEvent.java        # Event model
│   │   ├── PageEditWindow.java            # Detection logic
│   │   ├── EditWarAlert.java              # Alert model
│   │   └── EditWarStatus.java             # Status enum
│   ├── service/
│   │   ├── WikimediaEventParser.java      # JSON parsing
│   │   └── EditWarDetectionService.java   # Main detection service
│   ├── controller/
│   │   ├── ApiRealTimeChangesController.java  # SSE endpoint
│   │   └── TestDataController.java            # Test/demo endpoints
│   └── ApiRealTimeChangesConsumer.java    # Kafka consumer
└── README.md
```

## ⚙️ Configuration

### Producer (`kafka-producer-api/src/main/resources/application.properties`)
```properties
spring.kafka.producer.bootstrap-servers=localhost:9092
server.port=8080
```

### Consumer (`kafka-consumer-api/src/main/resources/application.properties`)
```properties
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=myGroup
server.port=8081
```

## 🔮 Future Enhancements

Potential improvements:
- [ ] Persist alerts to database
- [ ] Real-time dashboard with charts
- [ ] Email/Slack notifications for high-severity wars
- [ ] Machine learning for improved conflict prediction
- [ ] Support for multiple language Wikipedias
- [ ] Historical edit war analysis

## 🐛 Troubleshooting

### No events appearing?

1. Check Kafka is running: `bin/kafka-topics.sh --list --bootstrap-server localhost:9092`
2. Verify topic exists: Should see `wikimedia-stream-api`
3. Check producer logs for connection errors
4. Test Wikimedia URL: `curl -I https://stream.wikimedia.org/v2/stream/recentchange`

### No alerts appearing?

This is normal! Real edit wars are extremely rare. Use test endpoints instead:
```bash
curl -X POST http://localhost:8081/api/test/simulate-edit-war
```

### Port conflicts?

Change ports in `application.properties` files.

## 📝 License

This project is open source and available under the MIT License.

## 👤 Author

Eugene Paitoo

[My LinkedIn]("https://www.linkedin.com/in/eugene-paitoo/")  


## 🙏 Acknowledgments

- Wikimedia Foundation for providing the EventStreams API
- Spring/Apache Kafka communities

---

**Note**: This is a learning/portfolio project. For production Wikipedia monitoring, consider using Wikimedia's official tools like [ORES](https://www.mediawiki.org/wiki/ORES).