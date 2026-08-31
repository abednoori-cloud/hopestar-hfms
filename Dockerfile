# =====================================================================
# HopeStar HFMS - multi-stage Docker build
# Stage 1: build the jar with Maven (no local Maven install required)
# Stage 2: run it on a minimal JRE image
# =====================================================================

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Leverage Docker layer caching: resolve dependencies before copying source
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user
RUN groupadd -r hfms && useradd -r -g hfms hfms
RUN mkdir -p /app/data/documents /app/data/receipts /app/data/backups /var/log/hfms \
    && chown -R hfms:hfms /app /var/log/hfms

COPY --from=build /build/target/hfms.jar /app/hfms.jar
RUN chown hfms:hfms /app/hfms.jar

USER hfms

ENV HFMS_PROFILE=prod \
    SERVER_PORT=8080 \
    HFMS_DOCS_PATH=/app/data/documents \
    HFMS_RECEIPTS_PATH=/app/data/receipts \
    HFMS_BACKUP_PATH=/app/data/backups

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/hfms.jar"]
