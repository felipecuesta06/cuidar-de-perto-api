package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.Alerta;
import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import br.com.fiap.cuidardeperto.domain.TipoAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long>, JpaSpecificationExecutor<Alerta> {

    /** Projecao para a contagem por gravidade. */
    interface ResumoGravidade {
        Gravidade getGravidade();

        Long getTotal();
    }

    // ---- query methods derivados ----
    Page<Alerta> findByIdosoIdAndStatus(Long idosoId, StatusAlerta status, Pageable pageable);

    List<Alerta> findByIdosoIdAndStatusIn(Long idosoId, Collection<StatusAlerta> status);

    Optional<Alerta> findFirstByIdosoIdAndTipoAndStatus(Long idosoId, TipoAlerta tipo, StatusAlerta status);

    List<Alerta> findByGravidadeAndStatusAndEscalonadoFalseAndCriadoEmBefore(
            Gravidade gravidade, StatusAlerta status, LocalDateTime limite);

    List<Alerta> findByTipoAndGravidadeAndStatusAndCriadoEmBefore(
            TipoAlerta tipo, Gravidade gravidade, StatusAlerta status, LocalDateTime limite);

    /** Carrega o idoso junto na busca por Specification (evita N+1 na listagem). */
    @Override
    @EntityGraph(attributePaths = "idoso")
    Page<Alerta> findAll(Specification<Alerta> spec, Pageable pageable);

    // ---- @Query JPQL ----

    /**
     * Consulta critica do dominio: painel do cuidador com alertas ABERTOS de gravidade ALTA/CRITICA dos idosos
     * vinculados a ele. A gravidade e gravada como texto, entao a ordenacao usa CASE (CRITICA primeiro) em vez de
     * ORDER BY gravidade, que seria alfabetico.
     */
    @Query(value = """
            SELECT a FROM Alerta a
            JOIN FETCH a.idoso i
            WHERE a.status = br.com.fiap.cuidardeperto.domain.StatusAlerta.ABERTO
              AND a.gravidade IN (br.com.fiap.cuidardeperto.domain.Gravidade.ALTA,
                                  br.com.fiap.cuidardeperto.domain.Gravidade.CRITICA)
              AND i.id IN (SELECT c.id FROM Idoso c JOIN c.cuidadores cu WHERE cu.id = :cuidadorId)
            ORDER BY CASE a.gravidade
                       WHEN br.com.fiap.cuidardeperto.domain.Gravidade.CRITICA THEN 0
                       ELSE 1 END,
                     a.criadoEm ASC
            """,
            countQuery = """
            SELECT COUNT(a) FROM Alerta a
            WHERE a.status = br.com.fiap.cuidardeperto.domain.StatusAlerta.ABERTO
              AND a.gravidade IN (br.com.fiap.cuidardeperto.domain.Gravidade.ALTA,
                                  br.com.fiap.cuidardeperto.domain.Gravidade.CRITICA)
              AND a.idoso.id IN (SELECT c.id FROM Idoso c JOIN c.cuidadores cu WHERE cu.id = :cuidadorId)
            """)
    Page<Alerta> painelUrgente(@Param("cuidadorId") Long cuidadorId, Pageable pageable);

    /** Agregacao (GROUP BY): alertas por gravidade a partir de uma data (ex.: ultimas 24h). */
    @Query("SELECT a.gravidade AS gravidade, COUNT(a) AS total FROM Alerta a "
            + "WHERE a.criadoEm >= :desde GROUP BY a.gravidade")
    List<ResumoGravidade> contagemPorGravidade(@Param("desde") LocalDateTime desde);
}
