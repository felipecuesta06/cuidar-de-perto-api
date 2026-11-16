package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.Idoso;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;


public final class IdosoSpecs {

    private IdosoSpecs() {
    }

    public static Specification<Idoso> nomeContem(String nome) {
        return (root, query, cb) -> nome == null || nome.isBlank()
                ? null
                : cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%");
    }

    public static Specification<Idoso> condicaoContem(String condicao) {
        return (root, query, cb) -> condicao == null || condicao.isBlank()
                ? null
                : cb.like(cb.lower(root.get("condicoesMedicas")), "%" + condicao.trim().toLowerCase() + "%");
    }


    public static Specification<Idoso> idadeEntre(Integer idadeMin, Integer idadeMax) {
        return (root, query, cb) -> {
            LocalDate hoje = LocalDate.now();
            jakarta.persistence.criteria.Predicate p = null;
            if (idadeMin != null) {
                p = cb.lessThanOrEqualTo(root.get("dataNascimento"), hoje.minusYears(idadeMin));
            }
            if (idadeMax != null) {
                jakarta.persistence.criteria.Predicate max =
                        cb.greaterThan(root.get("dataNascimento"), hoje.minusYears(idadeMax + 1L));
                p = (p == null) ? max : cb.and(p, max);
            }
            return p;
        };
    }

    public static Specification<Idoso> ativo(Boolean ativo) {
        return (root, query, cb) -> ativo == null ? null : cb.equal(root.get("ativo"), ativo);
    }


    public static Specification<Idoso> doCuidador(Long cuidadorId) {
        return (root, query, cb) -> cb.equal(root.join("cuidadores").get("id"), cuidadorId);
    }
}
