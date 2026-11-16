package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.domain.Papel;
import br.com.fiap.cuidardeperto.dto.AuthDTO.CadastroRequest;
import br.com.fiap.cuidardeperto.dto.AuthDTO.UsuarioResponse;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.ResponsavelRequest;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.ResponsavelResponse;
import br.com.fiap.cuidardeperto.service.AuthService;
import br.com.fiap.cuidardeperto.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/responsaveis")
@RequiredArgsConstructor
@Tag(name = "Responsaveis")
public class ResponsavelController {

    private final PessoaService pessoaService;
    private final AuthService authService;

    @Operation(summary = "Cadastra um responsavel (admin ou cuidador; cria a conta de acesso)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Responsavel criado"),
                    @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")})
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUIDADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrar(new CadastroRequest(req.nome(), req.email(), req.senha(), Papel.RESPONSAVEL,
                req.telefone(), null));
    }

    @Operation(summary = "Lista responsaveis (paginado)",
            responses = {@ApiResponse(responseCode = "200", description = "Pagina de responsaveis")})
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUIDADOR')")
    public PagedModel<EntityModel<ResponsavelResponse>> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable,
            PagedResourcesAssembler<ResponsavelResponse> pagedAssembler) {
        return pagedAssembler.toModel(pessoaService.listarResponsaveis(pageable));
    }

    @Operation(summary = "Detalhe do responsavel (admin, cuidador ou o proprio)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Responsavel encontrado"),
                    @ApiResponse(responseCode = "403", description = "Sem permissao"),
                    @ApiResponse(responseCode = "404", description = "Nao encontrado")})
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CUIDADOR') or @acesso.ehOResponsavel(#id)")
    public ResponsavelResponse detalhar(@PathVariable Long id) {
        return pessoaService.buscarResponsavel(id);
    }

    @Operation(summary = "Atualiza o responsavel (admin ou o proprio)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Responsavel atualizado"),
                    @ApiResponse(responseCode = "403", description = "Sem permissao"),
                    @ApiResponse(responseCode = "404", description = "Nao encontrado")})
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @acesso.ehOResponsavel(#id)")
    public ResponsavelResponse atualizar(@PathVariable Long id, @Valid @RequestBody ResponsavelRequest req) {
        return pessoaService.atualizarResponsavel(id, req);
    }

    @Operation(summary = "Inativa o responsavel e bloqueia o login (admin)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Responsavel inativado"),
                    @ApiResponse(responseCode = "404", description = "Nao encontrado")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        pessoaService.inativarResponsavel(id);
        return ResponseEntity.noContent().build();
    }
}
