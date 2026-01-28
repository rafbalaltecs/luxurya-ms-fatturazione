package it.fatturazione.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fatture")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fattura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroFattura;

    @Column(nullable = false)
    private LocalDate dataFattura;

    @Column(nullable = false)
    private String codiceFiscaleCedente;

    @Column(nullable = false)
    private String denominazioneCedente;

    @Column(nullable = false)
    private String partitaIvaCedente;

    @Column(nullable = false)
    private String codiceFiscaleCessionario;

    private String partitaIvaCessionario;

    @Column(nullable = false)
    private String denominazioneCessionario;

    private String codiceDestinatario;

    private String pecDestinatario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal imponibile;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoFattura stato;

    @Column(length = 1000)
    private String xmlPath;

    @Column(length = 1000)
    private String xmlFirmatoPath;

    private String identificativoSdi;

    private LocalDateTime dataInvio;

    private LocalDateTime dataRicevutaConsegna;

    @Column(length = 2000)
    private String noteErrore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @Column(nullable = false)
    private LocalDateTime dataUltimaModifica;

    @PrePersist
    protected void onCreate() {
        dataCreazione = LocalDateTime.now();
        dataUltimaModifica = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataUltimaModifica = LocalDateTime.now();
    }

    public enum StatoFattura {
        BOZZA,
        XML_GENERATO,
        FIRMATA,
        INVIATA,
        CONSEGNATA,
        ACCETTATA,
        RIFIUTATA,
        SCARTATA,
        ERRORE
    }
}
