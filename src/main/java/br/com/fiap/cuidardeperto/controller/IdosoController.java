package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.domain.TipoSinal;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.AlertaResponse;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.IdosoRequest;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.IdosoResponse;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.VinculoRequest;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.VinculoResponse;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EstadoIdosoResponse;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EventoSensorRequest;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EventoSensorResponse;
import br.com.fiap.cuidardeperto.service.AlertaService;
import br.com.fiap.cuidardeperto.service.EstadoService;
import br.com.fiap.cuidardeperto.service.EventoSensorService;
import br.com.fiap.cuidardeperto.service.IdosoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/idosos")
@RequiredArgsConstructor
@Tag(name = "Idosos", description = "Cadastro, estado de saude, vinculos, historico de sensores e emergencia")
public class IdosoController {

    private final IdosoService idosoService;
    private final EstadoService estadoService;
    private final EventoSensorService eventoSensorService;
    private final AlertaService alertaService;
    private final IdosoModelAssembler assembler;
    private final AlertaModelAssembler alertaAssembler;

    @Operation(summary = "Cadastra idoso (cuidador logado passa a acompanha-lo)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Idoso criado"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "403", description = "Papel sem permissao"),
                    @ApiResponse(responseCode = "409", description = "CPF ou e-mail duplicado"),
                    @ApiResponse(responseCode = "422", description = "Regra de negocio violada")})
    @PostMapping
    @PreAuthorize("hasAnyRole('CUIDADOR','ADMIN')")
    public ResponseEntity<EntityModel<IdosoResponse>> criar(@Valid @RequestBody IdosoRequest req) {
        EntityModel<IdosoResponse> modelo = assembler.toModel(idosoService.criar(req));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @Operation(summary = "Lista paginada com filtros dinamicos (nome, faixa de idade, condicao, ativo)",
            description = "Cuidador ve somente os idosos vinculados a ele. CPF nunca aparece na listagem.",
            responses = {@ApiResponse(responseCode = "200", description = "Pagina de idosos com links HATEOAS")})
    @GetMapping
    @PreAuthorize("hasAnyRole('CUIDADOR','ADMIN')")
    public PagedModel<EntityModel<IdosoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Integer idadeMin,
            @RequestParam(required = false) Integer idadeMax,
            @RequestParam(required = false) String condicao,
            @RequestParam(required = false, defaultValue = "true") Boolean ativo,
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable,
            PagedResourcesAssembler<IdosoResponse> pagedAssembler) {
        Page<IdosoResponse> pagina = idosoService.listar(nome, idadeMin, idadeMax, condicao, ativo, pageable);
        return pagedAssembler.toModel(pagina, assembler);
    }

    @Operation(summary = "Detalhe do idoso com links HATEOAS (CPF mascarado)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Idoso encontrado"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso nao encontrado")})
    @GetMapping("/{id}")
    @PreAuthorize("@acesso.podeVer(#id)")
    public EntityModel<IdosoResponse> detalhar(@PathVariable Long id) {
        return assembler.toModel(idosoService.detalhar(id));
    }

    @Operation(summary = "Atualiza dados e limites de saude",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Idoso atualizado"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso nao encontrado"),
                    @ApiResponse(responseCode = "409", description = "CPF duplicado")})
    @PutMapping("/{id}")
    @PreAuthorize("@acesso.podeEditar(#id)")
    public EntityModel<IdosoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody IdosoRequest req) {
        return assembler.toModel(idosoService.atualizar(id, req));
    }

    @Operation(summary = "Inativa o idoso (exclusao logica; historico preservado)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Idoso inativado"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso nao encontrado")})
    @DeleteMapping("/{id}")
    @PreAuthorize("@acesso.podeEditar(#id)")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        idosoService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Estado atual: ultimo valor de cada sinal + alertas abertos (cacheado)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Estado do idoso"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso nao encontrado")})
    @GetMapping("/{id}/estado")
    @PreAuthorize("@acesso.podeVer(#id)")
    public EstadoIdosoResponse estado(@PathVariable Long id) {
        return estadoService.estadoDoIdoso(id);
    }

    @Operation(summary = "Historico de leituras de sensor, com filtro por tipo e periodo",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagina de leituras"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso")})
    @GetMapping("/{id}/eventos-sensor")
    @PreAuthorize("@acesso.podeVer(#id)")
    public PagedModel<EntityModel<EventoSensorResponse>> historico(
            @PathVariable Long id,
            @RequestParam(required = false) TipoSinal tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            PagedResourcesAssembler<EventoSensorResponse> pagedAssembler) {
        return pagedAssembler.toModel(eventoSensorService.historico(id, tipo, de, ate, pageable));
    }

    @Operation(summary = "Botao de panico: gera alerta CRITICO e notifica cuidador e todos os responsaveis",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Alerta critico criado/agregado"),
                    @ApiResponse(responseCode = "403", description = "Somente o proprio idoso")})
    @PostMapping("/{id}/emergencia")
    @PreAuthorize("@acesso.ehOIdoso(#id)")
    public ResponseEntity<EntityModel<AlertaResponse>> emergencia(@PathVariable Long id) {
        EventoSensorResponse evento = eventoSensorService.registrar(new EventoSensorRequest(
                id, TipoSinal.BOTAO_PANICO, 1.0, "evento", "app-idoso", LocalDateTime.now()));
        EntityModel<AlertaResponse> modelo = alertaAssembler.toModel(alertaService.buscar(evento.alertaId()));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    // ------------------------------------------------------------------ vinculos

    @Operation(summary = "Lista os responsaveis vinculados (por prioridade)",
            responses = {@ApiResponse(responseCode = "200", description = "Pagina de vinculos")})
    @GetMapping("/{id}/responsaveis")
    @PreAuthorize("@acesso.podeVer(#id)")
    public PagedModel<EntityModel<VinculoResponse>> responsaveis(
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "prioridadeContato") Pageable pageable,
            PagedResourcesAssembler<VinculoResponse> pagedAssembler) {
        return pagedAssembler.toModel(idosoService.listarResponsaveis(id, pageable));
    }

    @Operation(summary = "Vincula responsavel ao idoso (parentesco e prioridade de contato)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Vinculo criado"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso ou responsavel nao encontrado"),
                    @ApiResponse(responseCode = "409", description = "Responsavel ja vinculado")})
    @PostMapping("/{id}/responsaveis/{respId}")
    @PreAuthorize("@acesso.podeEditar(#id)")
    public ResponseEntity<VinculoResponse> vincular(@PathVariable Long id, @PathVariable Long respId,
                                                    @Valid @RequestBody VinculoRequest req) {
        return ResponseEntity.status(201).body(idosoService.vincularResponsavel(id, respId, req));
    }

    @Operation(summary = "Remove o vinculo do responsavel",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Vinculo removido"),
                    @ApiResponse(responseCode = "404", description = "Vinculo nao encontrado")})
    @DeleteMapping("/{id}/responsaveis/{respId}")
    @PreAuthorize("@acesso.podeEditar(#id)")
    public ResponseEntity<Void> desvincular(@PathVariable Long id, @PathVariable Long respId) {
        idosoService.desvincularResponsavel(id, respId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vincula outro cuidador ao idoso",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Cuidador vinculado"),
                    @ApiResponse(responseCode = "404", description = "Idoso ou cuidador nao encontrado")})
    @PostMapping("/{id}/cuidadores/{cuidadorId}")
    @PreAuthorize("@acesso.podeEditar(#id)")
    public ResponseEntity<Void> vincularCuidador(@PathVariable Long id, @PathVariable Long cuidadorId) {
        idosoService.vincularCuidador(id, cuidadorId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove o vinculo de um cuidador com o idoso",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Vinculo removido"),
                    @ApiResponse(responseCode = "404", description = "Vinculo nao encontrado")})
    @DeleteMapping("/{id}/cuidadores/{cuidadorId}")
    @PreAuthorize("@acesso.podeEditar(#id)")
    public ResponseEntity<Void> desvincularCuidador(@PathVariable Long id, @PathVariable Long cuidadorId) {
        idosoService.desvincularCuidador(id, cuidadorId);
        return ResponseEntity.noContent().build();
    }
}
