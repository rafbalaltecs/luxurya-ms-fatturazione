package it.fatturazione.repository;

import it.fatturazione.entity.NotificaSdi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificaSdiRepository extends JpaRepository<NotificaSdi, Long> {

    List<NotificaSdi> findByFatturaId(Long fatturaId);

    List<NotificaSdi> findByIdentificativoSdi(String identificativoSdi);

    List<NotificaSdi> findByTipoNotifica(NotificaSdi.TipoNotifica tipoNotifica);
}
