# 1. Build stage using Maven and JDK 17 (or Java 21)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy Maven wrapper & POM to leverage layer caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code and build the production JAR (skipping tests)
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Production Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Render sets $PORT dynamically)
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]