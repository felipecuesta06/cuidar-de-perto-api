package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.dto.LembreteDTO.LembreteRequest;
import br.com.fiap.cuidardeperto.dto.LembreteDTO.LembreteResponse;
import br.com.fiap.cuidardeperto.service.LembreteService;
import br.com.fiap.cuidardeperto.service.PageUtil;
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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Lembretes de remedio")
public class LembreteController {

    private final LembreteService lembreteService;

    @Operation(summary = "Cria lembrete de remedio",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Lembrete criado"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso nao encontrado")})
    @PostMapping("/idosos/{id}/lembretes")
    @PreAuthorize("@acesso.podeEditar(#id)")
    @ResponseStatus(HttpStatus.CREATED)
    public LembreteResponse criar(@PathVariable Long id, @Valid @RequestBody LembreteRequest req) {
        return lembreteService.criar(id, req);
    }

    @Operation(summary = "Lista lembretes do idoso (ativos e vigentes hoje, ou todos com ativos=false)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagina de lembretes"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso")})
    @GetMapping("/idosos/{id}/lembretes")
    @PreAuthorize("@acesso.podeVer(#id)")
    public PagedModel<EntityModel<LembreteResponse>> listar(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "true") boolean ativos,
            @ParameterObject @PageableDefault(size = 20, sort = "horario") Pageable pageable,
            PagedResourcesAssembler<LembreteResponse> pagedAssembler) {
        Page<LembreteResponse> pagina = ativos
                ? PageUtil.paginar(lembreteService.ativosDoDia(id), pageable)
                : lembreteService.todos(id, pageable);
        return pagedAssembler.toModel(pagina);
    }

    @Operation(summary = "Altera o lembrete",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lembrete atualizado"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Lembrete nao encontrado")})
    @PutMapping("/lembretes/{id}")
    @PreAuthorize("hasAnyRole('CUIDADOR','ADMIN')")
    public LembreteResponse atualizar(@PathVariable Long id, @Valid @RequestBody LembreteRequest req) {
        return lembreteService.atualizar(id, req);
    }

    @Operation(summary = "Remove o lembrete",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Lembrete removido"),
                    @ApiResponse(responseCode = "403", description = "Sem vinculo com o idoso"),
                    @ApiResponse(responseCode = "404", description = "Lembrete nao encontrado")})
    @DeleteMapping("/lembretes/{id}")
    @PreAuthorize("hasAnyRole('CUIDADOR','ADMIN')")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        lembreteService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registra a tomada do remedio (zera as omissoes seguidas)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tomada confirmada"),
                    @ApiResponse(responseCode = "403", description = "Somente o proprio idoso ou seu cuidador"),
                    @ApiResponse(responseCode = "404", description = "Lembrete nao encontrado")})
    @PatchMapping("/lembretes/{id}/confirmar")
    @PreAuthorize("hasAnyRole('IDOSO','CUIDADOR','ADMIN')")
    public LembreteResponse confirmar(@PathVariable Long id) {
        return lembreteService.confirmar(id);
    }
}
