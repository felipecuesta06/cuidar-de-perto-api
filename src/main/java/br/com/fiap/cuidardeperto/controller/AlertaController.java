package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.AlertaResponse;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.AlertaStatusRequest;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.ResumoGravidadeResponse;
import br.com.fiap.cuidardeperto.security.AcessoService;
import br.com.fiap.cuidardeperto.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Painel, filtros, detalhe com acoes HATEOAS e tratamento")
public class AlertaController {

    private final AlertaService alertaService;
    private final AlertaModelAssembler assembler;
    private final AcessoService acesso;

    @Operation(summary = "Lista alertas com filtros (gravidade, status, idoso, periodo)",
            description = "Restrita ao escopo do usuario: cuidador e responsavel veem apenas idosos vinculados.",
            responses = {@ApiResponse(responseCode = "200", description = "Pagina de alertas com links de acao")})
    @GetMapping
    @PreAuthorize("hasAnyRole('CUIDADOR','ADMIN','RESPONSAVEL')")
    public PagedModel<EntityModel<AlertaResponse>> listar(
            @RequestParam(required = false) Long idosoId,
            @RequestParam(required = false) Gravidade gravidade,
            @RequestParam(required = false) StatusAlerta status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            PagedResourcesAssembler<AlertaResponse> pagedAssembler) {
        return pagedAssembler.toModel(
                alertaService.listar(idosoId, gravidade, status, de, ate, pageable), assembler);
    }

    @Operation(summary = "Painel urgente do cuidador: alertas ABERTOS de gravidade ALTA/CRITICA (cacheado)",
            description = "CRITICA primeiro, depois os mais antigos. Apenas idosos vinculados ao cuidador logado.",
            responses = {@ApiResponse(responseCode = "200", description = "Pagina de alertas urgentes")})
    @GetMapping("/painel-urgente")
    @PreAuthorize("hasRole('CUIDADOR')")
    public PagedModel<EntityModel<AlertaResponse>> painelUrgente(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            PagedResourcesAssembler<AlertaResponse> pagedAssembler) {
        // A ordenacao ja e definida na consulta; so pagina e tamanho valem.
        Pageable somentePagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Long cuidadorId = acesso.cuidadorAtual().getId();
        return pagedAssembler.toModel(alertaService.painelUrgente(cuidadorId, somentePagina), assembler);
    }

    @Operation(summary = "Resumo: total de alertas por gravidade nas ultimas 24h (metricas do admin)",
            responses = {@ApiResponse(responseCode = "200", description = "Contagem por gravidade (GROUP BY)")})
    @GetMapping("/resumo")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ResumoGravidadeResponse> resumo() {
        return alertaService.resumoUltimas24h();
    }

    @Operation(summary = "Detalhe do alerta com links de acao conforme o status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Alerta encontrado"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso do alerta"),
                    @ApiResponse(responseCode = "404", description = "Alerta nao encontrado")})
    @GetMapping("/{id}")
    public EntityModel<AlertaResponse> detalhar(@PathVariable Long id) {
        return assembler.toModel(alertaService.buscar(id));
    }

    @Operation(summary = "Move o alerta para EM_ATENDIMENTO ou RESOLVIDO (resolucao exige observacao)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status atualizado"),
                    @ApiResponse(responseCode = "403", description = "Somente cuidador vinculado ou admin"),
                    @ApiResponse(responseCode = "404", description = "Alerta nao encontrado"),
                    @ApiResponse(responseCode = "422", description = "Transicao invalida ou observacao ausente")})
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUIDADOR','ADMIN')")
    public EntityModel<AlertaResponse> atualizarStatus(@PathVariable Long id,
                                                       @Valid @RequestBody AlertaStatusRequest req) {
        return assembler.toModel(alertaService.atualizarStatus(id, req.status(), req.observacao()));
    }
}
