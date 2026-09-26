# syntax=docker/dockerfile:1
# Build tools stay in the first stage; the final image contains only Java + the app.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src/ src/
# Tests already run in CI; local ./mvnw verify runs them too.
RUN ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S qalab && adduser -S -G qalab qalab
WORKDIR /app
COPY --from=build --chown=qalab:qalab /build/target/qa-practice-api-1.0.0.jar app.jar
USER qalab
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:${PORT}/api/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
