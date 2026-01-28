-- Script di inizializzazione database PostgreSQL
-- Sistema di Fatturazione Elettronica

-- Crea il database (esegui come superuser)
-- CREATE DATABASE fatturazione_db;
-- \c fatturazione_db;

-- Le tabelle verranno create automaticamente da Hibernate/JPA
-- Questo script contiene query utili per la gestione

-- ============================================
-- Query utili per la gestione
-- ============================================

-- Visualizza tutte le fatture
SELECT 
    id,
    numero_fattura,
    data_fattura,
    denominazione_cedente,
    denominazione_cessionario,
    totale,
    stato,
    identificativo_sdi,
    data_invio
FROM fatture
ORDER BY data_creazione DESC;

-- Fatture per stato
SELECT stato, COUNT(*) as totale
FROM fatture
GROUP BY stato;

-- Fatture inviate ma non ancora confermate (dopo 24 ore)
SELECT *
FROM fatture
WHERE stato = 'INVIATA'
AND data_invio < NOW() - INTERVAL '24 hours';

-- Statistiche mensili
SELECT 
    DATE_TRUNC('month', data_fattura) as mese,
    COUNT(*) as numero_fatture,
    SUM(imponibile) as totale_imponibile,
    SUM(iva) as totale_iva,
    SUM(totale) as totale_fatturato
FROM fatture
WHERE data_fattura >= DATE_TRUNC('year', CURRENT_DATE)
GROUP BY DATE_TRUNC('month', data_fattura)
ORDER BY mese DESC;

-- Fatture con errori
SELECT 
    numero_fattura,
    denominazione_cessionario,
    stato,
    note_errore,
    data_ultima_modifica
FROM fatture
WHERE stato IN ('ERRORE', 'SCARTATA', 'RIFIUTATA')
ORDER BY data_ultima_modifica DESC;

-- Notifiche ricevute per una fattura
SELECT 
    f.numero_fattura,
    n.tipo_notifica,
    n.data_ricezione,
    n.messaggio_notifica
FROM notifiche_sdi n
JOIN fatture f ON n.fattura_id = f.id
WHERE f.id = 1
ORDER BY n.data_ricezione DESC;

-- Tutte le notifiche recenti
SELECT 
    f.numero_fattura,
    n.tipo_notifica,
    n.identificativo_sdi,
    n.data_ricezione,
    n.messaggio_notifica
FROM notifiche_sdi n
JOIN fatture f ON n.fattura_id = f.id
ORDER BY n.data_ricezione DESC
LIMIT 50;

-- Report fatturazione per cliente
SELECT 
    denominazione_cessionario,
    COUNT(*) as numero_fatture,
    SUM(totale) as totale_fatturato,
    AVG(totale) as media_fattura,
    MAX(data_fattura) as ultima_fattura
FROM fatture
WHERE stato NOT IN ('BOZZA', 'ERRORE')
GROUP BY denominazione_cessionario
ORDER BY totale_fatturato DESC;

-- ============================================
-- Indici per migliorare le performance
-- ============================================

CREATE INDEX IF NOT EXISTS idx_fatture_numero ON fatture(numero_fattura);
CREATE INDEX IF NOT EXISTS idx_fatture_stato ON fatture(stato);
CREATE INDEX IF NOT EXISTS idx_fatture_data ON fatture(data_fattura);
CREATE INDEX IF NOT EXISTS idx_fatture_identificativo_sdi ON fatture(identificativo_sdi);
CREATE INDEX IF NOT EXISTS idx_fatture_cessionario ON fatture(denominazione_cessionario);
CREATE INDEX IF NOT EXISTS idx_notifiche_fattura_id ON notifiche_sdi(fattura_id);
CREATE INDEX IF NOT EXISTS idx_notifiche_identificativo_sdi ON notifiche_sdi(identificativo_sdi);
CREATE INDEX IF NOT EXISTS idx_notifiche_tipo ON notifiche_sdi(tipo_notifica);

-- ============================================
-- Backup e manutenzione
-- ============================================

-- Comando per backup (eseguire da shell)
-- pg_dump -U postgres -d fatturazione_db -F c -b -v -f backup_fatturazione_$(date +%Y%m%d).backup

-- Comando per restore (eseguire da shell)
-- pg_restore -U postgres -d fatturazione_db -v backup_fatturazione_YYYYMMDD.backup

-- Pulizia fatture vecchie in bozza (più di 1 anno)
-- ATTENZIONE: Eseguire solo se necessario
-- DELETE FROM fatture 
-- WHERE stato = 'BOZZA' 
-- AND data_creazione < NOW() - INTERVAL '1 year';

-- Vacuum per ottimizzare il database
VACUUM ANALYZE;

-- ============================================
-- Monitoraggio
-- ============================================

-- Dimensione del database
SELECT 
    pg_size_pretty(pg_database_size('fatturazione_db')) as dimensione_db;

-- Dimensione delle tabelle
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as dimensione
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Connessioni attive
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query_start
FROM pg_stat_activity
WHERE datname = 'fatturazione_db';
