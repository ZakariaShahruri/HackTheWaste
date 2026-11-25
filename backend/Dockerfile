FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Dependencies first, so code changes don't download them again
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp -DskipTests package && cp target/leuven-go-*.jar app.jar
# Class data sharing needs plain jars on the classpath instead of nested ones
RUN java -Djarmode=tools -jar app.jar extract --destination extracted

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 leuvengo
WORKDIR /app
COPY --from=build /build/extracted/ ./
# Start once (on the in-memory database) and save the loaded classes, so a cold
# start doesn't have to parse and verify them again
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar
USER leuvengo
# Size the heap from the container's memory limit rather than the host's
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:SharedArchiveFile=/app/app.jsa"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS "http://localhost:${PORT:-8080}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
