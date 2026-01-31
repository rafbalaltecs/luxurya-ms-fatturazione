package it.fatturazione.service;

import it.fatturazione.dto.FatturaRequestDTO;
import it.fatturazione.dto.FatturaResponseDTO;
import it.fatturazione.entity.Fattura;
import it.fatturazione.entity.NotificaSdi;
import it.fatturazione.exception.FatturaNotFoundException;
import it.fatturazione.repository.FatturaRepository;
import it.fatturazione.repository.NotificaSdiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FatturaService {

    private final FatturaRepository fatturaRepository;
    private final NotificaSdiRepository notificaSdiRepository;
    private final XmlGeneratorService xmlGeneratorService;
    private final FirmaDigitaleService firmaDigitaleService;
    private final SdiService sdiService;

    @Value("${fattura.storage.path}")
    private String storagePath;

    public List<FatturaResponseDTO> searchFattura(
            final BigDecimal from,
            final BigDecimal to
    ){
        final List<FatturaResponseDTO> responseDTOS = new ArrayList<>();
        final List<Fattura> fatturaList = fatturaRepository.findByTotalBetween(from , to);
        if(!fatturaList.isEmpty()){
            for(final Fattura fattura: fatturaList){
                responseDTOS.add(FatturaResponseDTO.fromEntity(fattura));
            }
        }
        return responseDTOS;
    }


    @Transactional
    public FatturaResponseDTO creaFattura(FatturaRequestDTO request) throws Exception {
        log.info("Creazione nuova fattura: {}", request.getNumeroFattura());

        // Verifica che il numero fattura non esista già
        if (fatturaRepository.existsByNumeroFattura(request.getNumeroFattura())) {
            throw new IllegalArgumentException("Fattura con numero " + request.getNumeroFattura() + " già esistente");
        }

        // Calcola i totali
        BigDecimal imponibile = request.getRiepilogoIva().getImponibile();
        BigDecimal iva = request.getRiepilogoIva().getImposta();
        BigDecimal totale = imponibile.add(iva);

        // Crea l'entità Fattura
        Fattura fattura = Fattura.builder()
                .numeroFattura(request.getNumeroFattura())
                .dataFattura(request.getDataFattura())
                .codiceFiscaleCedente(request.getCedente().getCodiceFiscale() != null ? 
                        request.getCedente().getCodiceFiscale() : request.getCedente().getPartitaIva())
                .denominazioneCedente(request.getCedente().getDenominazione())
                .partitaIvaCedente(request.getCedente().getPartitaIva())
                .codiceFiscaleCessionario(request.getCessionario().getCodiceFiscale())
                .partitaIvaCessionario(request.getCessionario().getPartitaIva())
                .denominazioneCessionario(request.getCessionario().getDenominazione())
                .codiceDestinatario(request.getCessionario().getCodiceDestinatario())
                .pecDestinatario(request.getCessionario().getPec())
                .imponibile(imponibile)
                .iva(iva)
                .totale(totale)
                .stato(Fattura.StatoFattura.BOZZA)
                .build();

        fattura = fatturaRepository.save(fattura);

        log.info("Fattura creata con successo - ID: {}", fattura.getId());
        return FatturaResponseDTO.fromEntity(fattura);
    }

    @Transactional
    public FatturaResponseDTO generaXml(Long fatturaId, FatturaRequestDTO request) throws Exception {
        log.info("Generazione XML per fattura ID: {}", fatturaId);

        Fattura fattura = fatturaRepository.findById(fatturaId)
                .orElseThrow(() -> new FatturaNotFoundException("Fattura non trovata con ID: " + fatturaId));

        try {
            // Genera il file XML
            String xmlPath = xmlGeneratorService.generaXml(request, fattura);

            // Aggiorna lo stato della fattura
            fattura.setXmlPath(xmlPath);
            fattura.setStato(Fattura.StatoFattura.XML_GENERATO);
            fattura = fatturaRepository.save(fattura);

            log.info("XML generato con successo per fattura ID: {}", fatturaId);
            return FatturaResponseDTO.fromEntity(fattura);

        } catch (Exception e) {
            fattura.setStato(Fattura.StatoFattura.ERRORE);
            fattura.setNoteErrore("Errore generazione XML: " + e.getMessage());
            fatturaRepository.save(fattura);
            throw e;
        }
    }

    @Transactional
    public FatturaResponseDTO firmaFattura(Long fatturaId) throws Exception {
        log.info("Firma digitale per fattura ID: {}", fatturaId);

        Fattura fattura = fatturaRepository.findById(fatturaId)
                .orElseThrow(() -> new FatturaNotFoundException("Fattura non trovata con ID: " + fatturaId));

        if (fattura.getXmlPath() == null) {
            throw new IllegalStateException("La fattura non ha un XML generato");
        }

        try {
            // Firma il file XML
            String xmlFirmatoPath = firmaDigitaleService.firmaFile(fattura.getXmlPath());

            // Aggiorna lo stato della fattura
            fattura.setXmlFirmatoPath(xmlFirmatoPath);
            fattura.setStato(Fattura.StatoFattura.FIRMATA);
            fattura = fatturaRepository.save(fattura);

            log.info("Fattura firmata con successo - ID: {}", fatturaId);
            return FatturaResponseDTO.fromEntity(fattura);

        } catch (Exception e) {
            fattura.setStato(Fattura.StatoFattura.ERRORE);
            fattura.setNoteErrore("Errore firma digitale: " + e.getMessage());
            fatturaRepository.save(fattura);
            throw e;
        }
    }

    @Transactional
    public FatturaResponseDTO inviaASdi(Long fatturaId) throws Exception {
        log.info("Invio fattura a SDI - ID: {}", fatturaId);

        Fattura fattura = fatturaRepository.findById(fatturaId)
                .orElseThrow(() -> new FatturaNotFoundException("Fattura non trovata con ID: " + fatturaId));

        if (fattura.getXmlFirmatoPath() == null) {
            throw new IllegalStateException("La fattura non è stata firmata");
        }

        try {
            // Estrai il nome del file
            String nomeFile = new File(fattura.getXmlFirmatoPath()).getName();

            // Invia a SDI
            String identificativoSdi = sdiService.inviaFattura(fattura.getXmlFirmatoPath(), nomeFile);

            // Aggiorna lo stato della fattura
            fattura.setIdentificativoSdi(identificativoSdi);
            fattura.setDataInvio(LocalDateTime.now());
            fattura.setStato(Fattura.StatoFattura.INVIATA);
            fattura = fatturaRepository.save(fattura);

            log.info("Fattura inviata con successo - ID: {}, IdentificativoSdI: {}", 
                    fatturaId, identificativoSdi);
            return FatturaResponseDTO.fromEntity(fattura);

        } catch (Exception e) {
            fattura.setStato(Fattura.StatoFattura.ERRORE);
            fattura.setNoteErrore("Errore invio SDI: " + e.getMessage());
            fatturaRepository.save(fattura);
            throw e;
        }
    }

    @Transactional
    public FatturaResponseDTO processoCompleto(FatturaRequestDTO request) throws Exception {
        log.info("Avvio processo completo per fattura: {}", request.getNumeroFattura());

        // 1. Crea la fattura
        FatturaResponseDTO fattura = creaFattura(request);

        // 2. Genera l'XML
        fattura = generaXml(fattura.getId(), request);

        // 3. Firma digitalmente
        fattura = firmaFattura(fattura.getId());

        // 4. Invia a SDI
        fattura = inviaASdi(fattura.getId());

        log.info("Processo completo completato con successo per fattura: {}", request.getNumeroFattura());
        return fattura;
    }

    @Transactional(readOnly = true)
    public FatturaResponseDTO getFattura(Long id) {
        Fattura fattura = fatturaRepository.findById(id)
                .orElseThrow(() -> new FatturaNotFoundException("Fattura non trovata con ID: " + id));
        return FatturaResponseDTO.fromEntity(fattura);
    }

    @Transactional(readOnly = true)
    public FatturaResponseDTO getFatturaByNumero(String numeroFattura) {
        Fattura fattura = fatturaRepository.findByNumeroFattura(numeroFattura)
                .orElseThrow(() -> new FatturaNotFoundException("Fattura non trovata con numero: " + numeroFattura));
        return FatturaResponseDTO.fromEntity(fattura);
    }

    @Transactional(readOnly = true)
    public List<FatturaResponseDTO> getAllFatture() {
        return fatturaRepository.findAll().stream()
                .map(FatturaResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FatturaResponseDTO> getFattureByStato(Fattura.StatoFattura stato) {
        return fatturaRepository.findByStato(stato).stream()
                .map(FatturaResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processaNotifica(String identificativoSdi, String nomeFileNotifica, 
                                 NotificaSdi.TipoNotifica tipoNotifica, String messaggioNotifica) throws Exception {
        log.info("Processamento notifica - IdentificativoSdI: {}, Tipo: {}", identificativoSdi, tipoNotifica);

        // Trova la fattura
        Fattura fattura = fatturaRepository.findByIdentificativoSdi(identificativoSdi)
                .orElseThrow(() -> new FatturaNotFoundException(
                        "Fattura non trovata con IdentificativoSdI: " + identificativoSdi));

        // Scarica la notifica da SDI
        byte[] notificaContent = sdiService.scaricaNotifica(identificativoSdi, nomeFileNotifica);

        // Salva la notifica su disco
        String notificaPath = storagePath + "/notifiche/" + nomeFileNotifica;
        File notificaDir = new File(storagePath + "/notifiche");
        if (!notificaDir.exists()) {
            notificaDir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(notificaPath)) {
            fos.write(notificaContent);
        }

        // Crea l'entità NotificaSdi
        NotificaSdi notifica = NotificaSdi.builder()
                .fattura(fattura)
                .tipoNotifica(tipoNotifica)
                .identificativoSdi(identificativoSdi)
                .dataRicezione(LocalDateTime.now())
                .messaggioNotifica(messaggioNotifica)
                .xmlNotificaPath(notificaPath)
                .build();

        notificaSdiRepository.save(notifica);

        // Aggiorna lo stato della fattura in base al tipo di notifica
        aggiornaStatoFattura(fattura, tipoNotifica, messaggioNotifica);

        log.info("Notifica processata con successo per fattura ID: {}", fattura.getId());
    }

    private void aggiornaStatoFattura(Fattura fattura, NotificaSdi.TipoNotifica tipoNotifica, 
                                     String messaggioNotifica) {
        switch (tipoNotifica) {
            case RICEVUTA_CONSEGNA:
                fattura.setStato(Fattura.StatoFattura.CONSEGNATA);
                fattura.setDataRicevutaConsegna(LocalDateTime.now());
                break;
            case NOTIFICA_ESITO:
                // Verifica se è accettata o rifiutata dal messaggio
                if (messaggioNotifica != null && messaggioNotifica.toLowerCase().contains("accettata")) {
                    fattura.setStato(Fattura.StatoFattura.ACCETTATA);
                } else {
                    fattura.setStato(Fattura.StatoFattura.RIFIUTATA);
                    fattura.setNoteErrore("Fattura rifiutata: " + messaggioNotifica);
                }
                break;
            case NOTIFICA_SCARTO:
                fattura.setStato(Fattura.StatoFattura.SCARTATA);
                fattura.setNoteErrore("Fattura scartata: " + messaggioNotifica);
                break;
            case NOTIFICA_MANCATA_CONSEGNA:
            case ATTESTAZIONE_TRASMISSIONE:
                fattura.setStato(Fattura.StatoFattura.ERRORE);
                fattura.setNoteErrore(messaggioNotifica);
                break;
            case NOTIFICA_DECORRENZA_TERMINI:
                fattura.setStato(Fattura.StatoFattura.ACCETTATA);
                break;
        }

        fatturaRepository.save(fattura);
    }

    @Transactional
    public void eliminaFattura(Long id) {
        log.info("Eliminazione fattura ID: {}", id);
        
        Fattura fattura = fatturaRepository.findById(id)
                .orElseThrow(() -> new FatturaNotFoundException("Fattura non trovata con ID: " + id));

        // Non permettere eliminazione di fatture già inviate
        if (fattura.getStato() != Fattura.StatoFattura.BOZZA && 
            fattura.getStato() != Fattura.StatoFattura.ERRORE) {
            throw new IllegalStateException("Non è possibile eliminare una fattura già inviata");
        }

        // Elimina i file associati
        if (fattura.getXmlPath() != null) {
            new File(fattura.getXmlPath()).delete();
        }
        if (fattura.getXmlFirmatoPath() != null) {
            new File(fattura.getXmlFirmatoPath()).delete();
        }

        fatturaRepository.delete(fattura);
        log.info("Fattura eliminata con successo - ID: {}", id);
    }
}
