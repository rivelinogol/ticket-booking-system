FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG SERVICE_MODULE
WORKDIR /workspace

COPY pom.xml .
COPY common common
COPY auth-service auth-service
COPY event-management-service event-management-service
COPY seat-inventory-service seat-inventory-service
COPY booking-service booking-service
COPY payment-service payment-service
COPY notification-service notification-service
COPY audit-service audit-service

RUN mvn -pl ${SERVICE_MODULE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre

ARG SERVICE_MODULE
WORKDIR /app

COPY --from=build /workspace/${SERVICE_MODULE}/target /tmp/target
RUN JAR_FILE="$(ls /tmp/target/*.jar | grep -v 'original' | head -n 1)" \
    && cp "${JAR_FILE}" /app/app.jar \
    && rm -rf /tmp/target

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
