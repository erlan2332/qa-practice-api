# syntax=docker/dockerfile:1
# Build tools stay in the first stage; the final image contains only Java + the app.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src/ src/
# Tests already run in CI; local ./mvnw verify runs them too.
RUN ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system qalab \
    && useradd --system --gid qalab --no-create-home --home-dir /app --shell /usr/sbin/nologin qalab
WORKDIR /app
COPY --from=build --chown=qalab:qalab /build/target/qa-practice-api-1.0.0.jar app.jar
USER qalab
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 CMD curl --fail --silent --show-error --max-time 4 -o /dev/null http://127.0.0.1:${PORT}/api/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
