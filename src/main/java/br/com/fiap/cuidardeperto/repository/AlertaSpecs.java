package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.Alerta;
import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;


public final class AlertaSpecs {

    private AlertaSpecs() {
    }

    public static Specification<Alerta> gravidade(Gravidade gravidade) {
        return (root, query, cb) -> gravidade == null ? null : cb.equal(root.get("gravidade"), gravidade);
    }

    public static Specification<Alerta> status(StatusAlerta status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Alerta> idoso(Long idosoId) {
        return (root, query, cb) -> idosoId == null ? null : cb.equal(root.get("idoso").get("id"), idosoId);
    }

    public static Specification<Alerta> periodo(LocalDateTime de, LocalDateTime ate) {
        return (root, query, cb) -> {
            Predicate p = null;
            if (de != null) {
                p = cb.greaterThanOrEqualTo(root.get("criadoEm"), de);
            }
            if (ate != null) {
                Predicate fim = cb.lessThanOrEqualTo(root.get("criadoEm"), ate);
                p = (p == null) ? fim : cb.and(p, fim);
            }
            return p;
        };
    }


    public static Specification<Alerta> doCuidador(Long cuidadorId) {
        return (root, query, cb) -> cb.equal(
                root.join("idoso").join("cuidadores").get("id"), cuidadorId);
    }

    /** Escopo de posse do responsavel: alertas dos idosos aos quais esta vinculado. */
    public static Specification<Alerta> doResponsavel(Long responsavelId) {
        return (root, query, cb) -> cb.equal(
                root.join("idoso").join("responsaveis").join("responsavel").get("id"), responsavelId);
    }
}
