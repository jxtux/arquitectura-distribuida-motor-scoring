ARG OTEL_AGENT_VERSION=2.18.1
FROM maven:3.9-eclipse-temurin-21 AS build
ARG OTEL_AGENT_VERSION
WORKDIR /workspace
COPY . .
RUN mvn -q -pl services/notification-service -am package -DskipTests \
 && mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:${OTEL_AGENT_VERSION} -DoutputDirectory=/tmp/otel \
 && cp /tmp/otel/opentelemetry-javaagent-${OTEL_AGENT_VERSION}.jar /tmp/otel/opentelemetry-javaagent.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/notification-service/target/notification-service-*.jar /app/app.jar
COPY --from=build /tmp/otel/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENTRYPOINT ["java","-jar","/app/app.jar"]
