package it.fatturazione.dto;

import it.fatturazione.entity.Fattura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FatturaResponseDTO {

    private Long id;
    private String numeroFattura;
    private LocalDate dataFattura;
    private String denominazioneCedente;
    private String denominazioneCessionario;
    private BigDecimal imponibile;
    private BigDecimal iva;
    private BigDecimal totale;
    private String stato;
    private String identificativoSdi;
    private LocalDateTime dataInvio;
    private LocalDateTime dataRicevutaConsegna;
    private String noteErrore;
    private LocalDateTime dataCreazione;
    private LocalDateTime dataUltimaModifica;

    public static FatturaResponseDTO fromEntity(Fattura fattura) {
        return FatturaResponseDTO.builder()
                .id(fattura.getId())
                .numeroFattura(fattura.getNumeroFattura())
                .dataFattura(fattura.getDataFattura())
                .denominazioneCedente(fattura.getDenominazioneCedente())
                .denominazioneCessionario(fattura.getDenominazioneCessionario())
                .imponibile(fattura.getImponibile())
                .iva(fattura.getIva())
                .totale(fattura.getTotale())
                .stato(fattura.getStato().name())
                .identificativoSdi(fattura.getIdentificativoSdi())
                .dataInvio(fattura.getDataInvio())
                .dataRicevutaConsegna(fattura.getDataRicevutaConsegna())
                .noteErrore(fattura.getNoteErrore())
                .dataCreazione(fattura.getDataCreazione())
                .dataUltimaModifica(fattura.getDataUltimaModifica())
                .build();
    }
}
