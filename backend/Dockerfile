# =============================================================================
# WorkHive Backend - Production Dockerfile for Cloud Platforms (Railway, Render)
# =============================================================================

# Stage 1: Build JAR using Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

# Copy Maven descriptor and source code
COPY pom.xml ./
COPY src ./src

# Install Maven & build production JAR without running tests
RUN apt-get update && apt-get install -y maven \
    && mvn clean package -DskipTests \
    && rm -rf /var/lib/apt/lists/*

# Stage 2: Lightweight Production JRE 21 Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-privileged system user for container security
RUN groupadd -r workhive && useradd -r -g workhive workhive

# Copy compiled artifact from builder stage
COPY --from=builder /build/target/workhive-backend-1.0.0.jar /app/app.jar

RUN chown -R workhive:workhive /app
USER workhive

# Cloud platforms like Railway dynamically set $PORT
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
