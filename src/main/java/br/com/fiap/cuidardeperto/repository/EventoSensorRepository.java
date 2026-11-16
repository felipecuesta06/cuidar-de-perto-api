package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.EventoSensor;
import br.com.fiap.cuidardeperto.domain.TipoSinal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventoSensorRepository extends JpaRepository<EventoSensor, Long> {

    /** @Query JPQL: filtros combinados + ordenacao, que o nome do metodo nao expressa bem. */
    @Query("SELECT e FROM EventoSensor e WHERE e.idoso.id = :idosoId AND e.tipo = :tipo "
            + "AND e.dataHora >= :desde AND e.dataHora <= :ate ORDER BY e.dataHora DESC")
    Page<EventoSensor> historicoPorTipo(@Param("idosoId") Long idosoId,
                                        @Param("tipo") TipoSinal tipo,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("ate") LocalDateTime ate,
                                        Pageable pageable);

    @Query("SELECT e FROM EventoSensor e WHERE e.idoso.id = :idosoId "
            + "AND e.dataHora >= :desde AND e.dataHora <= :ate ORDER BY e.dataHora DESC")
    Page<EventoSensor> historico(@Param("idosoId") Long idosoId,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("ate") LocalDateTime ate,
                                 Pageable pageable);

    /** Ultima leitura de um sinal (usa o indice idoso_id + data_hora). */
    Optional<EventoSensor> findFirstByIdosoIdAndTipoOrderByDataHoraDesc(Long idosoId, TipoSinal tipo);
}
