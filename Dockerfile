# Multi-stage build per ottimizzare la dimensione dell'immagine

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia i file di configurazione Maven
COPY pom.xml .
COPY src ./src
COPY certs ./app/certs

# Compila l'applicazione
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crea le directory necessarie
RUN mkdir -p /var/fatture /var/fatture/notifiche /tmp/fatture


# Copia il jar dall'immagine di build
COPY --from=build /app/target/*.jar app.jar

# Esponi la porta dell'applicazione
EXPOSE 8080

# Aggiungi un healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Configura JVM options
ENV JAVA_OPTS="-Xmx1024m -Xms512m"

# Avvia l'applicazione
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
