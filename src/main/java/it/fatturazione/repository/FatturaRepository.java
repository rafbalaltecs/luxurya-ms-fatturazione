package it.fatturazione.repository;

import it.fatturazione.entity.Fattura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FatturaRepository extends JpaRepository<Fattura, Long> {

    Optional<Fattura> findByNumeroFattura(String numeroFattura);

    Optional<Fattura> findByIdentificativoSdi(String identificativoSdi);

    List<Fattura> findByStato(Fattura.StatoFattura stato);

    List<Fattura> findByDataFatturaBetween(LocalDate dataInizio, LocalDate dataFine);

    @Query("SELECT f FROM Fattura f where f.totale >= :from AND f.totale<= :to")
    List<Fattura> findByTotalBetween(@Param("from") final BigDecimal from, @Param("to") final BigDecimal to);

    @Query("SELECT f FROM Fattura f WHERE f.stato = 'INVIATA' AND f.dataInvio < :dataLimite")
    List<Fattura> findFattureInAttesaDiRisposta(java.time.LocalDateTime dataLimite);

    boolean existsByNumeroFattura(String numeroFattura);
}
