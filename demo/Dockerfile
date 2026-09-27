FROM maven:3.9-eclipse-temurin-23 AS build

WORKDIR /workspace

# Optimizar memoria durante la compilación
ENV MAVEN_OPTS="-Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Cache de dependencias
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiar fuentes y compilar
COPY src ./src
RUN mvn -B clean package -DskipTests

# Imagen de ejecución ligera basada en Alpine
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

COPY --from=build /workspace/target/app-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# Limitación de memoria y optimización de recursos:
# -Xms128m: consumo inicial bajo
# -Xmx512m: límite máximo de heap (evita que Java reserve GBs según la RAM del host)
# -Xss512k: optimiza el tamaño de la pila de hilos
# -XX:+UseSerialGC: recolector de basura ligero ideal para contenedores
# -XX:MaxMetaspaceSize=192m: controla el crecimiento del metaspace
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
