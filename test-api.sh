#!/bin/bash

# Script di test per il Sistema di Fatturazione Elettronica
# Esegue una serie di test sulle API REST

BASE_URL="http://localhost:8080/api/fatture"
CONTENT_TYPE="Content-Type: application/json"

echo "==================================="
echo "Test Sistema Fatturazione Elettronica"
echo "==================================="
echo ""

# Colori per output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funzione per stampare risultati
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2${NC}"
    fi
}

# Test 1: Verifica che l'applicazione sia attiva
echo -e "${YELLOW}Test 1: Verifica applicazione${NC}"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL)
if [ "$RESPONSE" = "200" ]; then
    print_result 0 "Applicazione attiva"
else
    print_result 1 "Applicazione non risponde (HTTP $RESPONSE)"
    exit 1
fi
echo ""

# Test 2: Crea una fattura
echo -e "${YELLOW}Test 2: Creazione fattura${NC}"
CREATE_RESPONSE=$(curl -s -X POST $BASE_URL \
  -H "$CONTENT_TYPE" \
  -d '{
    "numeroFattura": "TEST/001",
    "dataFattura": "2025-01-08",
    "cedente": {
      "denominazione": "Azienda Test SRL",
      "partitaIva": "12345678901",
      "indirizzo": "Via Roma 123",
      "cap": "20100",
      "comune": "Milano",
      "provincia": "MI",
      "nazione": "IT"
    },
    "cessionario": {
      "denominazione": "Cliente Test",
      "codiceFiscale": "RSSMRA80A01H501U",
      "indirizzo": "Via Verdi 456",
      "cap": "00100",
      "comune": "Roma",
      "provincia": "RM",
      "nazione": "IT",
      "codiceDestinatario": "0000000"
    },
    "dettaglioRighe": [
      {
        "numeroLinea": 1,
        "descrizione": "Test prodotto",
        "quantita": 1,
        "unitaMisura": "pz",
        "prezzoUnitario": 100.00,
        "aliquotaIva": 22.00
      }
    ],
    "riepilogoIva": {
      "aliquotaIva": 22.00,
      "imponibile": 100.00,
      "imposta": 22.00
    }
  }')

FATTURA_ID=$(echo $CREATE_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

if [ ! -z "$FATTURA_ID" ]; then
    print_result 0 "Fattura creata con ID: $FATTURA_ID"
else
    print_result 1 "Errore nella creazione della fattura"
    echo "Risposta: $CREATE_RESPONSE"
    exit 1
fi
echo ""

# Test 3: Recupera la fattura creata
echo -e "${YELLOW}Test 3: Recupero fattura${NC}"
GET_RESPONSE=$(curl -s -X GET "$BASE_URL/$FATTURA_ID")
NUMERO_FATTURA=$(echo $GET_RESPONSE | grep -o '"numeroFattura":"[^"]*"' | cut -d'"' -f4)

if [ "$NUMERO_FATTURA" = "TEST/001" ]; then
    print_result 0 "Fattura recuperata correttamente"
else
    print_result 1 "Errore nel recupero della fattura"
fi
echo ""

# Test 4: Lista tutte le fatture
echo -e "${YELLOW}Test 4: Lista fatture${NC}"
LIST_RESPONSE=$(curl -s -X GET $BASE_URL)
FATTURE_COUNT=$(echo $LIST_RESPONSE | grep -o '"id":' | wc -l)

if [ $FATTURE_COUNT -gt 0 ]; then
    print_result 0 "Trovate $FATTURE_COUNT fatture"
else
    print_result 1 "Nessuna fattura trovata"
fi
echo ""

# Test 5: Filtra fatture per stato
echo -e "${YELLOW}Test 5: Filtra fatture per stato BOZZA${NC}"
FILTER_RESPONSE=$(curl -s -X GET "$BASE_URL/stato/BOZZA")
BOZZE_COUNT=$(echo $FILTER_RESPONSE | grep -o '"id":' | wc -l)

if [ $BOZZE_COUNT -gt 0 ]; then
    print_result 0 "Trovate $BOZZE_COUNT fatture in bozza"
else
    print_result 1 "Nessuna fattura in bozza trovata"
fi
echo ""

# Test 6: Cerca fattura per numero
echo -e "${YELLOW}Test 6: Cerca fattura per numero${NC}"
SEARCH_RESPONSE=$(curl -s -X GET "$BASE_URL/numero/TEST%2F001")
SEARCH_ID=$(echo $SEARCH_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

if [ "$SEARCH_ID" = "$FATTURA_ID" ]; then
    print_result 0 "Fattura trovata per numero"
else
    print_result 1 "Fattura non trovata per numero"
fi
echo ""

# Test 7: Tenta di creare una fattura duplicata
echo -e "${YELLOW}Test 7: Test validazione (fattura duplicata)${NC}"
DUPLICATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL \
  -H "$CONTENT_TYPE" \
  -d '{
    "numeroFattura": "TEST/001",
    "dataFattura": "2025-01-08",
    "cedente": {
      "denominazione": "Azienda Test SRL",
      "partitaIva": "12345678901",
      "indirizzo": "Via Roma 123",
      "cap": "20100",
      "comune": "Milano",
      "provincia": "MI",
      "nazione": "IT"
    },
    "cessionario": {
      "denominazione": "Cliente Test",
      "codiceFiscale": "RSSMRA80A01H501U",
      "indirizzo": "Via Verdi 456",
      "cap": "00100",
      "comune": "Roma",
      "provincia": "RM",
      "nazione": "IT",
      "codiceDestinatario": "0000000"
    },
    "dettaglioRighe": [
      {
        "numeroLinea": 1,
        "descrizione": "Test",
        "quantita": 1,
        "unitaMisura": "pz",
        "prezzoUnitario": 100.00,
        "aliquotaIva": 22.00
      }
    ],
    "riepilogoIva": {
      "aliquotaIva": 22.00,
      "imponibile": 100.00,
      "imposta": 22.00
    }
  }')

HTTP_CODE=$(echo "$DUPLICATE_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "201" ]; then
    print_result 0 "Validazione duplicati funziona correttamente (HTTP $HTTP_CODE)"
else
    print_result 1 "Validazione duplicati non funziona"
fi
echo ""

# Test 8: Elimina la fattura di test
echo -e "${YELLOW}Test 8: Eliminazione fattura${NC}"
DELETE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/$FATTURA_ID")

if [ "$DELETE_RESPONSE" = "204" ]; then
    print_result 0 "Fattura eliminata con successo"
else
    print_result 1 "Errore nell'eliminazione della fattura (HTTP $DELETE_RESPONSE)"
fi
echo ""

# Test 9: Verifica che la fattura sia stata eliminata
echo -e "${YELLOW}Test 9: Verifica eliminazione${NC}"
VERIFY_DELETE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/$FATTURA_ID")

if [ "$VERIFY_DELETE" = "404" ]; then
    print_result 0 "Fattura correttamente eliminata"
else
    print_result 1 "Fattura ancora presente dopo eliminazione"
fi
echo ""

echo "==================================="
echo -e "${GREEN}Test completati!${NC}"
echo "==================================="
