# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY kafka-consumer-api/pom.xml kafka-consumer-api/
COPY kafka-producer-api/pom.xml kafka-producer-api/

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -pl kafka-consumer-api -am

# Copy source code
COPY kafka-consumer-api/src kafka-consumer-api/src

# Build the JAR (skip tests for faster builds)
RUN mvn package -pl kafka-consumer-api -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

COPY --from=build /app/kafka-consumer-api/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]

