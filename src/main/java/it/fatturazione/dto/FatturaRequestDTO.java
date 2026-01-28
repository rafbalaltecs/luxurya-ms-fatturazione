package it.fatturazione.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FatturaRequestDTO {

    @NotBlank(message = "Il numero fattura è obbligatorio")
    private String numeroFattura;

    @NotNull(message = "La data fattura è obbligatoria")
    private LocalDate dataFattura;

    @NotNull(message = "I dati del cedente sono obbligatori")
    private CedenteDTO cedente;

    @NotNull(message = "I dati del cessionario sono obbligatori")
    private CessionarioDTO cessionario;

    @NotEmpty(message = "Deve essere presente almeno una riga di dettaglio")
    private List<DettaglioRigaDTO> dettaglioRighe;

    @NotNull(message = "I dati di riepilogo sono obbligatori")
    private RiepilogoIvaDTO riepilogoIva;

    private DatiPagamentoDTO datiPagamento;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CedenteDTO {
        @NotBlank
        private String denominazione;
        
        @NotBlank
        @Pattern(regexp = "^[0-9]{11}$", message = "Partita IVA non valida")
        private String partitaIva;
        
        private String codiceFiscale;
        
        @NotBlank
        private String indirizzo;
        
        @NotBlank
        private String cap;
        
        @NotBlank
        private String comune;
        
        @NotBlank
        @Size(min = 2, max = 2)
        private String provincia;
        
        @NotBlank
        @Size(min = 2, max = 2)
        private String nazione;
        
        private String telefono;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CessionarioDTO {
        @NotBlank
        private String denominazione;
        
        private String partitaIva;
        
        @NotBlank
        private String codiceFiscale;
        
        @NotBlank
        private String indirizzo;
        
        @NotBlank
        private String cap;
        
        @NotBlank
        private String comune;
        
        @NotBlank
        @Size(min = 2, max = 2)
        private String provincia;
        
        @NotBlank
        @Size(min = 2, max = 2)
        private String nazione;
        
        @Size(min = 7, max = 7)
        private String codiceDestinatario;
        
        @Email
        private String pec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DettaglioRigaDTO {
        @NotNull
        @Min(1)
        private Integer numeroLinea;
        
        @NotBlank
        private String descrizione;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal quantita;
        
        @NotBlank
        private String unitaMisura;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal prezzoUnitario;
        
        @NotNull
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        private BigDecimal aliquotaIva;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiepilogoIvaDTO {
        @NotNull
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        private BigDecimal aliquotaIva;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal imponibile;
        
        @NotNull
        @DecimalMin(value = "0.00")
        private BigDecimal imposta;
        
        private String natura; // Per operazioni esenti/non imponibili
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatiPagamentoDTO {
        @NotBlank
        private String condizioniPagamento; // TP01, TP02, TP03
        
        @NotNull
        private DettaglioPagamentoDTO dettaglioPagamento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DettaglioPagamentoDTO {
        @NotBlank
        private String modalitaPagamento; // MP01-MP23
        
        @NotNull
        private LocalDate dataScadenza;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal importoPagamento;
        
        private String iban;
        private String istitutoFinanziario;
    }
}
