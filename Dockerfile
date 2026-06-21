FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system fleetmatch && adduser --system --ingroup fleetmatch fleetmatch

COPY --from=build /app/target/*.jar /app/fleetmatch-backend.jar

USER fleetmatch

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/fleetmatch-backend.jar"]
