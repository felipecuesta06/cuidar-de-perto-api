package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.AlertaResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;


@Component
public class AlertaModelAssembler implements RepresentationModelAssembler<AlertaResponse, EntityModel<AlertaResponse>> {

    @Override
    public EntityModel<AlertaResponse> toModel(AlertaResponse alerta) {
        EntityModel<AlertaResponse> modelo = EntityModel.of(alerta,
                linkTo(AlertaController.class).slash(alerta.id()).withSelfRel(),
                linkTo(IdosoController.class).slash(alerta.idosoId()).withRel("idoso"));

        if (alerta.status() != StatusAlerta.RESOLVIDO && usuarioPodeAgir()) {
            String statusUri = linkTo(AlertaController.class).slash(alerta.id()).slash("status").toString();
            if (alerta.status() == StatusAlerta.ABERTO) {
                modelo.add(org.springframework.hateoas.Link.of(statusUri, "assumir")
                        .withTitle("PATCH com {\"status\":\"EM_ATENDIMENTO\"}"));
            }
            modelo.add(org.springframework.hateoas.Link.of(statusUri, "resolver")
                    .withTitle("PATCH com {\"status\":\"RESOLVIDO\",\"observacao\":\"...\"}"));
        }
        return modelo;
    }

    private boolean usuarioPodeAgir() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUIDADOR") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
