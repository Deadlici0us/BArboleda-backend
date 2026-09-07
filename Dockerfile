# Build with ./mvnw (PLAN.md #7); never bare mvn so wrapper + .mvn/ stay authoritative.
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY mvnw ./
COPY .mvn/ ./.mvn/
# pom.xml before src/: dependency:go-offline caches ~/.m2 so source edits
# never re-download the graph; config/ sits with pom so checkstyle files
# do not invalidate the dependency layer either.
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -DskipTests
COPY config/ ./config/
COPY src ./src
RUN ./mvnw -B package -DskipTests && \
    rm -f target/original-*.jar && \
    java -Djarmode=tools -jar $(ls target/*.jar | grep -v original | head -n 1) extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app
# Layered boot (PLAN.md #7): only the application layer invalidates on code
# changes; unpacked classes also start faster than nested-JAR execution.
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./
EXPOSE 8080
# No HEALTHCHECK curl here (alpine jre ships no curl): orchestrator probes
# /actuator/health/liveness and /actuator/health/readiness (PLAN.md #7).
# JarLauncher boots the unpacked layers (no nested-JAR); InitialRAMPercentage
# avoids heap-resize latency at boot, Max caps the heap.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "org.springframework.boot.loader.launch.JarLauncher"]