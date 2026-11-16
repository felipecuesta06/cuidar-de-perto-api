package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.dto.IdosoDTO.IdosoResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/** HATEOAS do Idoso: self, estado, lembretes, eventos-sensor, alertas e responsaveis. */
@Component
public class IdosoModelAssembler implements RepresentationModelAssembler<IdosoResponse, EntityModel<IdosoResponse>> {

    @Override
    public EntityModel<IdosoResponse> toModel(IdosoResponse idoso) {
        Long id = idoso.id();
        String alertas = linkTo(AlertaController.class).toUriComponentsBuilder()
                .queryParam("idosoId", id).build().toUriString();
        return EntityModel.of(idoso,
                linkTo(IdosoController.class).slash(id).withSelfRel(),
                linkTo(IdosoController.class).slash(id).slash("estado").withRel("estado"),
                linkTo(IdosoController.class).slash(id).slash("lembretes").withRel("lembretes"),
                linkTo(IdosoController.class).slash(id).slash("eventos-sensor").withRel("eventos-sensor"),
                Link.of(alertas, "alertas"),
                linkTo(IdosoController.class).slash(id).slash("responsaveis").withRel("responsaveis"));
    }
}
