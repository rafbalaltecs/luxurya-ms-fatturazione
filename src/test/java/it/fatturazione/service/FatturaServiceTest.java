package it.fatturazione.service;

import it.fatturazione.dto.FatturaRequestDTO;
import it.fatturazione.dto.FatturaResponseDTO;
import it.fatturazione.entity.Fattura;
import it.fatturazione.repository.FatturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FatturaServiceTest {

    @Mock
    private FatturaRepository fatturaRepository;

    @Mock
    private XmlGeneratorService xmlGeneratorService;

    @Mock
    private FirmaDigitaleService firmaDigitaleService;

    @Mock
    private SdiService sdiService;

    @InjectMocks
    private FatturaService fatturaService;

    private FatturaRequestDTO requestDTO;
    private Fattura fattura;

    @BeforeEach
    void setUp() {
        // Prepara i dati di test
        requestDTO = FatturaRequestDTO.builder()
                .numeroFattura("2026/001")
                .dataFattura(LocalDate.now())
                .cedente(FatturaRequestDTO.CedenteDTO.builder()
                        .denominazione("Azienda Test SRL")
                        .partitaIva("12345678901")
                        .indirizzo("Via Test 1")
                        .cap("20100")
                        .comune("Milano")
                        .provincia("MI")
                        .nazione("IT")
                        .build())
                .cessionario(FatturaRequestDTO.CessionarioDTO.builder()
                        .denominazione("Cliente Test")
                        .codiceFiscale("RSSMRA80A01H501U")
                        .indirizzo("Via Cliente 1")
                        .cap("00100")
                        .comune("Roma")
                        .provincia("RM")
                        .nazione("IT")
                        .codiceDestinatario("0000000")
                        .build())
                .riepilogoIva(FatturaRequestDTO.RiepilogoIvaDTO.builder()
                        .aliquotaIva(new BigDecimal("22.00"))
                        .imponibile(new BigDecimal("100.00"))
                        .imposta(new BigDecimal("22.00"))
                        .build())
                .build();

        fattura = Fattura.builder()
                .id(1L)
                .numeroFattura("2026/001")
                .dataFattura(LocalDate.now())
                .denominazioneCedente("Azienda Test SRL")
                .denominazioneCessionario("Cliente Test")
                .imponibile(new BigDecimal("100.00"))
                .iva(new BigDecimal("22.00"))
                .totale(new BigDecimal("122.00"))
                .stato(Fattura.StatoFattura.BOZZA)
                .build();
    }

    @Test
    void testCreaFattura() throws Exception {
        // Arrange
        when(fatturaRepository.existsByNumeroFattura(anyString())).thenReturn(false);
        when(fatturaRepository.save(any(Fattura.class))).thenReturn(fattura);

        // Act
        FatturaResponseDTO result = fatturaService.creaFattura(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("2026/001", result.getNumeroFattura());
        assertEquals("BOZZA", result.getStato());
        verify(fatturaRepository, times(1)).save(any(Fattura.class));
    }

    @Test
    void testCreaFatturaDuplicata() {
        // Arrange
        when(fatturaRepository.existsByNumeroFattura(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            fatturaService.creaFattura(requestDTO);
        });
        
        verify(fatturaRepository, never()).save(any(Fattura.class));
    }

    @Test
    void testGetFattura() {
        // Arrange
        when(fatturaRepository.findById(1L)).thenReturn(Optional.of(fattura));

        // Act
        FatturaResponseDTO result = fatturaService.getFattura(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("2026/001", result.getNumeroFattura());
    }
}
