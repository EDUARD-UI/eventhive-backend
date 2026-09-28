FROM maven:3.9-eclipse-temurin-23 AS build

WORKDIR /workspace

ENV MAVEN_OPTS="-Xmx384m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xms128m", \
    "-Xmx300m", \
    "-Xss512k", \
    "-XX:+UseSerialGC", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:ActiveProcessorCount=1", \
    "-jar", \
    "/app/app.jar"]
