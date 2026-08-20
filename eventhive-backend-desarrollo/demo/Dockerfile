FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# Cache de dependencias (acelera rebuilds)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiar fuentes y compilar
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/demo-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]