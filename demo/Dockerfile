FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

ENV MAVEN_OPTS="-Xmx320m -XX:MaxMetaspaceSize=128m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xms32m", \
    "-Xmx160m", \
    "-Xss256k", \
    "-XX:+UseSerialGC", \
    "-XX:MaxMetaspaceSize=96m", \
    "-XX:ReservedCodeCacheSize=24m", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:ActiveProcessorCount=1", \
    "-jar", \
    "/app/app.jar"]
