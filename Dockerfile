# =============================================================================
# Stage 1: Build
# =============================================================================
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw package -DskipTests -B

# Extract the application with launcher for optimized startup
RUN java -Djarmode=tools -jar target/*.jar extract --launcher --destination extracted

# =============================================================================
# Stage 2: Runtime
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

WORKDIR /app

# Copy the extracted application (includes BOOT-INF, META-INF, and org directories)
COPY --from=builder /app/extracted/ ./

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080 8081

# Health check (management port)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
