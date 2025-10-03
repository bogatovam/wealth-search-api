# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom files for dependency caching
COPY pom.xml .
COPY wealth-search-engine-model/pom.xml wealth-search-engine-model/
COPY wealth-search-engine-api/pom.xml wealth-search-engine-api/
COPY wealth-search-engine-db/pom.xml wealth-search-engine-db/
COPY wealth-search-engine-impl/pom.xml wealth-search-engine-impl/
COPY wealth-search-engine-web/pom.xml wealth-search-engine-web/
COPY wealth-search-engine-application/pom.xml wealth-search-engine-application/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY wealth-search-engine-model/src wealth-search-engine-model/src
COPY wealth-search-engine-api/src wealth-search-engine-api/src
COPY wealth-search-engine-db/src wealth-search-engine-db/src
COPY wealth-search-engine-impl/src wealth-search-engine-impl/src
COPY wealth-search-engine-web/src wealth-search-engine-web/src
COPY wealth-search-engine-application/src wealth-search-engine-application/src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/wealth-search-engine-application/target/wealth-search-engine-application-*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
