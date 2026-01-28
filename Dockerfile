# Multi-stage build per ottimizzare la dimensione dell'immagine
WORKDIR /app

# Copia i file di configurazione Maven
COPY certs ./app/certs

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Crea le directory necessarie
RUN mkdir -p /var/fatture /var/fatture/notifiche /tmp/fatture

# Copia il jar dall'immagine di build
COPY app.jar app.jar

# Esponi la porta dell'applicazione
EXPOSE 8080

# Configura JVM options
ENV JAVA_OPTS="-Xmx1024m -Xms512m"

# Avvia l'applicazione
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
