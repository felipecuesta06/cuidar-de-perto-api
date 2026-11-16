package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.domain.Papel;
import br.com.fiap.cuidardeperto.dto.AuthDTO.CadastroRequest;
import br.com.fiap.cuidardeperto.dto.AuthDTO.UsuarioResponse;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.CuidadorRequest;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.CuidadorResponse;
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
@RequestMapping("/api/cuidadores")
@RequiredArgsConstructor
@Tag(name = "Cuidadores")
public class CuidadorController {

    private final PessoaService pessoaService;
    private final AuthService authService;

    @Operation(summary = "Admin cadastra um cuidador (cria a conta de acesso)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Cuidador criado"),
                    @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")})
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrar(new CadastroRequest(req.nome(), req.email(), req.senha(), Papel.CUIDADOR,
                req.telefone(), req.registroProfissional()));
    }

    @Operation(summary = "Lista cuidadores (paginado)",
            responses = {@ApiResponse(responseCode = "200", description = "Pagina de cuidadores")})
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PagedModel<EntityModel<CuidadorResponse>> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable,
            PagedResourcesAssembler<CuidadorResponse> pagedAssembler) {
        return pagedAssembler.toModel(pessoaService.listarCuidadores(pageable));
    }

    @Operation(summary = "Detalhe do cuidador (admin ou o proprio)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cuidador encontrado"),
                    @ApiResponse(responseCode = "403", description = "Sem permissao"),
                    @ApiResponse(responseCode = "404", description = "Nao encontrado")})
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @acesso.ehOCuidador(#id)")
    public CuidadorResponse detalhar(@PathVariable Long id) {
        return pessoaService.buscarCuidador(id);
    }

    @Operation(summary = "Atualiza o cuidador (admin ou o proprio)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cuidador atualizado"),
                    @ApiResponse(responseCode = "403", description = "Sem permissao"),
                    @ApiResponse(responseCode = "404", description = "Nao encontrado")})
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @acesso.ehOCuidador(#id)")
    public CuidadorResponse atualizar(@PathVariable Long id, @Valid @RequestBody CuidadorRequest req) {
        return pessoaService.atualizarCuidador(id, req);
    }

    @Operation(summary = "Inativa o cuidador e bloqueia o login (admin)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Cuidador inativado"),
                    @ApiResponse(responseCode = "404", description = "Nao encontrado")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        pessoaService.inativarCuidador(id);
        return ResponseEntity.noContent().build();
    }
}
