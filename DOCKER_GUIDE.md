# Guida Docker per Sistema Fatturazione Elettronica

## Prerequisiti

- Docker installato (versione 20.10 o superiore)
- Docker Compose installato (versione 2.0 o superiore)
- Certificato digitale PKCS#12 per la firma

## Preparazione

### 1. Crea la directory per i certificati

```bash
mkdir -p certs
```

### 2. Copia il tuo certificato nella directory certs

```bash
cp /path/to/tuo-certificato.p12 certs/certificato.p12
```

### 3. Configura le variabili d'ambiente

Crea un file `.env` nella root del progetto:

```bash
cat > .env << EOF
# Password del keystore
KEYSTORE_PASSWORD=tua_password_keystore

# Alias della chiave
KEY_ALIAS=tuo_alias

# Password della chiave
KEY_PASSWORD=tua_password_chiave

# Codice trasmittente SDI
SDI_TRASMITTENTE_CODICE=IL_TUO_CODICE
EOF
```

**IMPORTANTE**: Aggiungi `.env` al `.gitignore` per non committare password!

## Avvio dell'applicazione

### Metodo 1: Usando Docker Compose (consigliato)

```bash
# Avvia tutti i servizi
docker-compose up -d

# Verifica che i container siano attivi
docker-compose ps

# Visualizza i log
docker-compose logs -f app
```

### Metodo 2: Build e run manuale

```bash
# Build dell'immagine
docker build -t fatturazione-elettronica:latest .

# Run del container (assicurati che PostgreSQL sia già avviato)
docker run -d \
  --name fatturazione-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/fatturazione_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -v $(pwd)/certs:/app/certs:ro \
  -v fatture_data:/var/fatture \
  fatturazione-elettronica:latest
```

## Verifica del funzionamento

```bash
# Verifica che l'applicazione sia attiva
curl http://localhost:8080/actuator/health

# Test di una chiamata API
curl http://localhost:8080/api/fatture
```

## Accesso ai servizi

- **Applicazione**: http://localhost:8080
- **PgAdmin** (gestione database): http://localhost:5050
  - Email: admin@fatturazione.it
  - Password: admin

## Comandi utili

### Gestione dei container

```bash
# Arresta tutti i servizi
docker-compose down

# Arresta e rimuovi i volumi (ATTENZIONE: cancella i dati!)
docker-compose down -v

# Riavvia solo l'applicazione
docker-compose restart app

# Visualizza i log in tempo reale
docker-compose logs -f app

# Accedi al container dell'applicazione
docker-compose exec app sh

# Accedi al database PostgreSQL
docker-compose exec postgres psql -U fatturazione_user -d fatturazione_db
```

### Backup e restore

```bash
# Backup del database
docker-compose exec postgres pg_dump -U fatturazione_user fatturazione_db > backup.sql

# Restore del database
docker-compose exec -T postgres psql -U fatturazione_user -d fatturazione_db < backup.sql

# Backup dei file fatture
docker run --rm -v fatture_data:/data -v $(pwd):/backup alpine \
  tar czf /backup/fatture_backup_$(date +%Y%m%d).tar.gz -C /data .
```

### Pulizia

```bash
# Rimuovi immagini non utilizzate
docker image prune -a

# Rimuovi volumi non utilizzati
docker volume prune

# Rimuovi tutto (container, volumi, network)
docker-compose down -v --rmi all
```

## Troubleshooting

### L'applicazione non si avvia

1. Verifica i log:
```bash
docker-compose logs app
```

2. Verifica che PostgreSQL sia in esecuzione:
```bash
docker-compose ps postgres
```

3. Verifica la connessione al database:
```bash
docker-compose exec postgres pg_isready
```

### Errore di connessione al database

Verifica che le credenziali nel `docker-compose.yml` siano corrette e che PostgreSQL sia completamente avviato prima dell'applicazione.

### Errore di firma digitale

1. Verifica che il certificato sia presente:
```bash
docker-compose exec app ls -la /app/certs/
```

2. Verifica le variabili d'ambiente:
```bash
docker-compose exec app env | grep FIRMA
```

### Porta già in uso

Se la porta 8080 è già occupata, modifica la porta nel `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Usa la porta 8081 invece di 8080
```

## Configurazione per produzione

Per un ambiente di produzione:

1. **Usa secrets per le password**:
```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt
  keystore_password:
    file: ./secrets/keystore_password.txt
```

2. **Configura un reverse proxy** (es. Nginx) davanti all'applicazione

3. **Abilita HTTPS** usando certificati SSL

4. **Configura backup automatici** con cron job

5. **Usa l'URL di produzione SDI**:
```yaml
SDI_WS_URL: https://sdi-ws.fatturapa.it/ricevi_file
```

6. **Limita le risorse**:
```yaml
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 1G
    reservations:
      memory: 512M
```

## Monitoraggio

### Health check

L'applicazione espone un endpoint di health check:
```bash
curl http://localhost:8080/actuator/health
```

### Metriche

Aggiungi Spring Boot Actuator endpoints per monitoring:
```bash
curl http://localhost:8080/actuator/metrics
```

### Log aggregation

Per produzione, considera l'uso di:
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Prometheus + Grafana
- Cloud logging services

## Note di sicurezza

1. **Non committare il file `.env`** nel repository
2. **Usa Docker secrets** per password in produzione
3. **Limita l'accesso al container** usando user non privilegiati
4. **Aggiorna regolarmente** le immagini Docker
5. **Scansiona le immagini** per vulnerabilità:
```bash
docker scan fatturazione-elettronica:latest
```

## Supporto

Per problemi o domande:
1. Controlla i log: `docker-compose logs app`
2. Verifica la configurazione nel `docker-compose.yml`
3. Consulta la documentazione Spring Boot e Docker
