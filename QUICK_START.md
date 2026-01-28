# Quick Start - Sistema Fatturazione Elettronica

Guida rapida per avviare il sistema in 5 minuti.

## Opzione 1: Avvio con Docker (Consigliato)

### Prerequisiti
- Docker e Docker Compose installati
- Certificato digitale PKCS#12

### Passi

1. **Prepara il certificato**
```bash
mkdir certs
cp /path/to/tuo-certificato.p12 certs/certificato.p12
```

2. **Configura le variabili d'ambiente**
```bash
cat > .env << EOF
KEYSTORE_PASSWORD=tua_password
KEY_ALIAS=tuo_alias
KEY_PASSWORD=tua_password_chiave
SDI_TRASMITTENTE_CODICE=IL_TUO_CODICE
EOF
```

3. **Avvia i servizi**
```bash
docker-compose up -d
```

4. **Verifica che sia tutto attivo**
```bash
docker-compose ps
curl http://localhost:8080/api/fatture
```

✅ **Fatto!** L'applicazione è su http://localhost:8080

---

## Opzione 2: Avvio locale (Sviluppo)

### Prerequisiti
- JDK 17
- Maven 3.6+
- PostgreSQL 12+

### Passi

1. **Crea il database**
```bash
psql -U postgres
CREATE DATABASE fatturazione_db;
\q
```

2. **Configura application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fatturazione_db
spring.datasource.username=postgres
spring.datasource.password=your_password

firma.keystore.path=file:/path/to/certificato.p12
firma.keystore.password=your_password
firma.key.alias=your_alias
firma.key.password=your_key_password

sdi.trasmittente.codice=IL_TUO_CODICE
```

3. **Compila e avvia**
```bash
mvn clean install
mvn spring-boot:run
```

✅ **Fatto!** L'applicazione è su http://localhost:8080

---

## Test Rapido

### Crea una fattura completa

```bash
curl -X POST http://localhost:8080/api/fatture/processo-completo \
  -H "Content-Type: application/json" \
  -d '{
    "numeroFattura": "2025/001",
    "dataFattura": "2025-01-08",
    "cedente": {
      "denominazione": "Mia Azienda SRL",
      "partitaIva": "12345678901",
      "indirizzo": "Via Test 1",
      "cap": "20100",
      "comune": "Milano",
      "provincia": "MI",
      "nazione": "IT"
    },
    "cessionario": {
      "denominazione": "Cliente Test",
      "codiceFiscale": "RSSMRA80A01H501U",
      "indirizzo": "Via Cliente 1",
      "cap": "00100",
      "comune": "Roma",
      "provincia": "RM",
      "nazione": "IT",
      "codiceDestinatario": "0000000"
    },
    "dettaglioRighe": [{
      "numeroLinea": 1,
      "descrizione": "Servizio di test",
      "quantita": 1,
      "unitaMisura": "pz",
      "prezzoUnitario": 100.00,
      "aliquotaIva": 22.00
    }],
    "riepilogoIva": {
      "aliquotaIva": 22.00,
      "imponibile": 100.00,
      "imposta": 22.00
    }
  }'
```

### Visualizza tutte le fatture

```bash
curl http://localhost:8080/api/fatture
```

---

## Endpoint Principali

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/api/fatture/processo-completo` | Crea, genera XML, firma e invia |
| POST | `/api/fatture` | Crea solo la fattura |
| GET | `/api/fatture` | Lista tutte le fatture |
| GET | `/api/fatture/{id}` | Dettaglio fattura |
| POST | `/api/fatture/{id}/genera-xml` | Genera XML |
| POST | `/api/fatture/{id}/firma` | Firma digitale |
| POST | `/api/fatture/{id}/invia` | Invia a SDI |
| DELETE | `/api/fatture/{id}` | Elimina fattura |

---

## Troubleshooting Rapido

### L'app non si avvia
```bash
# Verifica i log
docker-compose logs app
# oppure
mvn spring-boot:run
```

### Errore database
```bash
# Verifica che PostgreSQL sia attivo
docker-compose ps postgres
# oppure
pg_isready
```

### Errore firma digitale
- Verifica che il certificato esista
- Controlla password e alias
- Assicurati che sia formato PKCS#12

---

## Prossimi Passi

1. Leggi il [README.md](README.md) completo
2. Consulta [ESEMPI_API.md](ESEMPI_API.md) per esempi dettagliati
3. Vedi [DOCKER_GUIDE.md](DOCKER_GUIDE.md) per Docker avanzato
4. Esegui i test: `./test-api.sh`

---

## Supporto

- Documentazione completa: [README.md](README.md)
- Esempi API: [ESEMPI_API.md](ESEMPI_API.md)
- SQL utilities: [database-utils.sql](database-utils.sql)

**Buon lavoro con il sistema di fatturazione elettronica! 🚀**
