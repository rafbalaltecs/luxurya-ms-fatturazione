package it.fatturazione.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifiche_sdi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificaSdi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fattura_id", nullable = false)
    private Fattura fattura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotifica tipoNotifica;

    @Column(nullable = false)
    private String identificativoSdi;

    @Column(nullable = false)
    private LocalDateTime dataRicezione;

    @Column(length = 5000)
    private String messaggioNotifica;

    @Column(length = 2000)
    private String xmlNotificaPath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @PrePersist
    protected void onCreate() {
        dataCreazione = LocalDateTime.now();
    }

    public enum TipoNotifica {
        RICEVUTA_CONSEGNA,      // RC - Ricevuta di consegna
        NOTIFICA_MANCATA_CONSEGNA,  // MC - Mancata consegna
        NOTIFICA_SCARTO,        // NS - Notifica di scarto
        NOTIFICA_ESITO,         // NE - Notifica esito cedente/prestatore
        ATTESTAZIONE_TRASMISSIONE,  // AT - Attestazione di trasmissione con impossibilità di recapito
        NOTIFICA_DECORRENZA_TERMINI  // DT - Notifica di decorrenza termini
    }
}
