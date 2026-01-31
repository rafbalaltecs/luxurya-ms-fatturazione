package it.fatturazione.controller;

import it.fatturazione.dto.FatturaRequestDTO;
import it.fatturazione.dto.FatturaResponseDTO;
import it.fatturazione.entity.Fattura;
import it.fatturazione.service.FatturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/fatture")
@RequiredArgsConstructor
@Slf4j
public class FatturaController {

    private final FatturaService fatturaService;

    @PostMapping
    public ResponseEntity<FatturaResponseDTO> creaFattura(@Valid @RequestBody FatturaRequestDTO request) {
        try {
            log.info("Richiesta creazione fattura: {}", request.getNumeroFattura());
            FatturaResponseDTO response = fatturaService.creaFattura(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Errore durante la creazione della fattura", e);
            throw new RuntimeException("Errore durante la creazione della fattura: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/genera-xml")
    public ResponseEntity<FatturaResponseDTO> generaXml(
            @PathVariable Long id,
            @Valid @RequestBody FatturaRequestDTO request) {
        try {
            log.info("Richiesta generazione XML per fattura ID: {}", id);
            FatturaResponseDTO response = fatturaService.generaXml(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore durante la generazione dell'XML", e);
            throw new RuntimeException("Errore durante la generazione dell'XML: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/firma")
    public ResponseEntity<FatturaResponseDTO> firmaFattura(@PathVariable Long id) {
        try {
            log.info("Richiesta firma digitale per fattura ID: {}", id);
            FatturaResponseDTO response = fatturaService.firmaFattura(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore durante la firma della fattura", e);
            throw new RuntimeException("Errore durante la firma della fattura: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/invia")
    public ResponseEntity<FatturaResponseDTO> inviaASdi(@PathVariable Long id) {
        try {
            log.info("Richiesta invio a SDI per fattura ID: {}", id);
            FatturaResponseDTO response = fatturaService.inviaASdi(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore durante l'invio a SDI", e);
            throw new RuntimeException("Errore durante l'invio a SDI: " + e.getMessage());
        }
    }

    @PostMapping("/processo-completo")
    public ResponseEntity<FatturaResponseDTO> processoCompleto(@Valid @RequestBody FatturaRequestDTO request) {
        try {
            log.info("Richiesta processo completo per fattura: {}", request.getNumeroFattura());
            FatturaResponseDTO response = fatturaService.processoCompleto(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Errore durante il processo completo", e);
            throw new RuntimeException("Errore durante il processo completo: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FatturaResponseDTO> getFattura(@PathVariable Long id) {
        log.info("Richiesta dettaglio fattura ID: {}", id);
        FatturaResponseDTO response = fatturaService.getFattura(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/numero/{numeroFattura}")
    public ResponseEntity<FatturaResponseDTO> getFatturaByNumero(@PathVariable String numeroFattura) {
        log.info("Richiesta dettaglio fattura numero: {}", numeroFattura);
        FatturaResponseDTO response = fatturaService.getFatturaByNumero(numeroFattura);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<FatturaResponseDTO>> searchFrombeetwen(
            @RequestParam(value = "from") BigDecimal from,
            @RequestParam(value = "to") BigDecimal to,
            @RequestParam(value = "target", required = false) BigDecimal target
            ) {
        return ResponseEntity.ok(fatturaService.searchFattura(from, to));
    }

    @GetMapping
    public ResponseEntity<List<FatturaResponseDTO>> getAllFatture() {
        log.info("Richiesta elenco tutte le fatture");
        List<FatturaResponseDTO> response = fatturaService.getAllFatture();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stato/{stato}")
    public ResponseEntity<List<FatturaResponseDTO>> getFattureByStato(@PathVariable String stato) {
        log.info("Richiesta fatture per stato: {}", stato);
        try {
            Fattura.StatoFattura statoFattura = Fattura.StatoFattura.valueOf(stato.toUpperCase());
            List<FatturaResponseDTO> response = fatturaService.getFattureByStato(statoFattura);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Stato fattura non valido: {}", stato);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminaFattura(@PathVariable Long id) {
        try {
            log.info("Richiesta eliminazione fattura ID: {}", id);
            fatturaService.eliminaFattura(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Errore durante l'eliminazione della fattura", e);
            throw new RuntimeException("Errore durante l'eliminazione della fattura: " + e.getMessage());
        }
    }
}
