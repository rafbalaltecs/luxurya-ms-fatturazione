# Stage Runtime (single-stage build perché il JAR è già buildato da Jenkins)
FROM eclipse-temurin:17-jre-alpine

# Crea un utente non-root per sicurezza
RUN addgroup -S spring && adduser -S spring -G spring

# Crea le directory necessarie
RUN mkdir -p /var/fatture /var/fatture/notifiche /tmp/fatture && \
    chown -R spring:spring /var/fatture /tmp/fatture

# Imposta la directory di lavoro
WORKDIR /app

# Copia il JAR (viene copiato da Jenkins sul server)
COPY --chown=spring:spring app.jar app.jar

# Copia i certificati
COPY --chown=spring:spring certs/ /app/certs/

# Cambia all'utente non-root
USER spring:spring

# Esponi la porta dell'applicazione
EXPOSE 8080

# Configura JVM options
ENV JAVA_OPTS="-Xmx1024m -Xms512m"

# Avvia l'applicazione
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]