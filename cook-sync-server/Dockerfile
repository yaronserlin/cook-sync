# syntax=docker/dockerfile:1

# Build context is the repository root (see docker-compose.yml) so this stage can see
# both cooksync-DTOs and cook-sync-server — the server depends on the DTOs jar being
# installed into the local Maven repo, the same way the README has developers do by hand.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY cooksync-DTOs/pom.xml cooksync-DTOs/pom.xml
COPY cooksync-DTOs/src cooksync-DTOs/src
RUN mvn -f cooksync-DTOs/pom.xml -B install -DskipTests

COPY cook-sync-server/pom.xml cook-sync-server/pom.xml
RUN mvn -f cook-sync-server/pom.xml -B dependency:go-offline

COPY cook-sync-server/src cook-sync-server/src
RUN mvn -f cook-sync-server/pom.xml -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S cooksync && adduser -S cooksync -G cooksync
COPY --from=build /workspace/cook-sync-server/target/*.jar app.jar
USER cooksync:cooksync

EXPOSE 8080

# wget comes from busybox, already in this alpine base - no extra package needed. --start-period
# gives Flyway migrations + JVM startup time to finish before a slow first check counts as a
# failure. A service/compose file with no healthcheck of its own (see docker-compose.yml) picks
# this one up automatically; Render's own "Health Check Path" dashboard setting is separate and
# still needs to be pointed at /actuator/health there.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
