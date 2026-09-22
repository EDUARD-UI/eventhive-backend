FROM maven:3.9-eclipse-temurin-23 AS build

WORKDIR /workspace

# Cache de dependencias
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiar fuentes y compilar
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- CAMBIO AQUÍ: Usamos alpine para reducir el consumo del S.O. ---
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# --- CAMBIO AQUÍ: Limitamos la memoria del Heap y optimizamos el GC ---
ENTRYPOINT ["java", "-Xmx300m", "-Xms200m", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
