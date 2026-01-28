# Sistema di Fatturazione Elettronica

Sistema completo per l'integrazione con il Sistema di Interscambio (SDI) italiano per la fatturazione elettronica.

## Caratteristiche

- ✅ Generazione XML FatturaPA versione 1.2
- ✅ Firma digitale P7M (CAdES)
- ✅ Invio tramite web service SDI
- ✅ Gestione notifiche SDI
- ✅ Persistenza su PostgreSQL
- ✅ API REST complete
- ✅ Validazione dati

## Tecnologie Utilizzate

- **Java 17**
- **Spring Boot 3.2.1**
- **PostgreSQL** - Database
- **Apache CXF** - Web Services SOAP
- **BouncyCastle** - Firma digitale
- **JAXB** - Marshalling/Unmarshalling XML
- **Maven** - Build tool

## Prerequisiti

- JDK 17 o superiore
- PostgreSQL 12 o superiore
- Maven 3.6 o superiore
- Certificato digitale qualificato (formato PKCS#12)

## Configurazione

### 1. Database PostgreSQL

Crea il database:

```sql
CREATE DATABASE fatturazione_db;
```

### 2. Application Properties

Modifica il file `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/fatturazione_db
spring.datasource.username=il_tuo_username
spring.datasource.password=la_tua_password

# SDI (Produzione o Test)
sdi.ws.url=https://testservizi.fatturapa.it/ricevi_file
sdi.trasmittente.codice=IL_TUO_CODICE_TRASMITTENTE

# Certificato Firma Digitale
firma.keystore.path=file:/path/to/certificato.p12
firma.keystore.password=password_keystore
firma.key.alias=alias_chiave
firma.key.password=password_chiave

# Storage
fattura.storage.path=/var/fatture
```

### 3. Certificato Digitale

Posiziona il tuo certificato PKCS#12 nella directory specificata in `firma.keystore.path`.

## Compilazione e Avvio

```bash
# Compila il progetto
mvn clean install

# Avvia l'applicazione
mvn spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

## API Endpoints

### Creare una Fattura

```http
POST /api/fatture
Content-Type: application/json

{
  "numeroFattura": "2025/001",
  "dataFattura": "2025-01-08",
  "cedente": {
    "denominazione": "Azienda SRL",
    "partitaIva": "12345678901",
    "indirizzo": "Via Roma 1",
    "cap": "20100",
    "comune": "Milano",
    "provincia": "MI",
    "nazione": "IT"
  },
  "cessionario": {
    "denominazione": "Cliente SPA",
    "codiceFiscale": "RSSMRA80A01H501U",
    "indirizzo": "Via Verdi 10",
    "cap": "00100",
    "comune": "Roma",
    "provincia": "RM",
    "nazione": "IT",
    "codiceDestinatario": "ABCDEFG"
  },
  "dettaglioRighe": [
    {
      "numeroLinea": 1,
      "descrizione": "Servizio di consulenza",
      "quantita": 1,
      "unitaMisura": "giorni",
      "prezzoUnitario": 100.00,
      "aliquotaIva": 22.00
    }
  ],
  "riepilogoIva": {
    "aliquotaIva": 22.00,
    "imponibile": 100.00,
    "imposta": 22.00
  }
}
```

### Processo Completo (Crea + Genera XML + Firma + Invia)

```http
POST /api/fatture/processo-completo
Content-Type: application/json

[stesso body dell'endpoint POST /api/fatture]
```

### Generare XML

```http
POST /api/fatture/{id}/genera-xml
Content-Type: application/json

[stesso body della creazione]
```

### Firmare la Fattura

```http
POST /api/fatture/{id}/firma
```

### Inviare a SDI

```http
POST /api/fatture/{id}/invia
```

### Ottenere una Fattura

```http
GET /api/fatture/{id}
GET /api/fatture/numero/{numeroFattura}
```

### Elencare Fatture

```http
GET /api/fatture
GET /api/fatture/stato/{stato}
```

Stati possibili: `BOZZA`, `XML_GENERATO`, `FIRMATA`, `INVIATA`, `CONSEGNATA`, `ACCETTATA`, `RIFIUTATA`, `SCARTATA`, `ERRORE`

### Eliminare una Fattura

```http
DELETE /api/fatture/{id}
```

## Flusso di Utilizzo

1. **Crea la fattura**: `POST /api/fatture`
2. **Genera l'XML**: `POST /api/fatture/{id}/genera-xml`
3. **Firma digitalmente**: `POST /api/fatture/{id}/firma`
4. **Invia a SDI**: `POST /api/fatture/{id}/invia`

Oppure usa il processo completo:

```http
POST /api/fatture/processo-completo
```

## Gestione Notifiche SDI

Le notifiche SDI vengono processate automaticamente. Tipi di notifiche:

- **RC** - Ricevuta di consegna
- **MC** - Mancata consegna
- **NS** - Notifica di scarto
- **NE** - Notifica esito (accettazione/rifiuto)
- **AT** - Attestazione di trasmissione
- **DT** - Decorrenza termini

## Struttura del Progetto

```
src/
├── main/
│   ├── java/it/fatturazione/
│   │   ├── config/          # Configurazioni
│   │   ├── controller/      # REST Controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # Entità JPA
│   │   ├── exception/      # Eccezioni custom
│   │   ├── repository/     # Repository JPA
│   │   ├── service/        # Business Logic
│   │   └── FatturazioneElettronicaApplication.java
│   └── resources/
│       ├── application.properties
│       └── xsd/            # Schema XSD FatturaPA
└── test/                   # Test unitari
```

## Test

Esegui i test con:

```bash
mvn test
```

## Ambiente di Test SDI

Per testare l'integrazione, usa l'ambiente di test fornito dall'Agenzia delle Entrate:

- **URL Test**: `https://testservizi.fatturapa.it/ricevi_file`
- **Documentazione**: https://www.fatturapa.gov.it/

## Note Importanti

1. **Certificato**: È necessario un certificato digitale qualificato per la firma
2. **Codice Trasmittente**: Deve essere registrato presso SDI
3. **Ambiente Test**: Usa l'ambiente di test SDI prima di passare in produzione
4. **Backup**: Implementa una strategia di backup per i file XML e le fatture
5. **Logging**: I log vengono salvati per il troubleshooting

## Sicurezza

- Le password nel file properties devono essere cifrate in produzione
- Usa HTTPS per le comunicazioni
- Proteggi il certificato digitale
- Implementa autenticazione e autorizzazione per le API

## Troubleshooting

### Errore di connessione a PostgreSQL
Verifica che il database sia in esecuzione e le credenziali siano corrette.

### Errore di firma digitale
Controlla che:
- Il certificato sia valido e non scaduto
- Il formato sia PKCS#12
- Password e alias siano corretti

### Errore invio SDI
Verifica:
- URL SDI corretto (test o produzione)
- Codice trasmittente valido
- XML conforme agli standard FatturaPA

## Riferimenti

- [Documentazione FatturaPA](https://www.fatturapa.gov.it/)
- [Specifiche Tecniche SDI](https://www.fatturapa.gov.it/it/norme-e-regole/documentazione-fattura-elettronica/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [BouncyCastle](https://www.bouncycastle.org/)

## Licenza

Questo progetto è fornito come esempio educativo.

## Contatti

Per supporto o domande, contatta il team di sviluppo.
