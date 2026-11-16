package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.dto.AuthDTO.CadastroRequest;
import br.com.fiap.cuidardeperto.dto.AuthDTO.LoginRequest;
import br.com.fiap.cuidardeperto.dto.AuthDTO.TokenResponse;
import br.com.fiap.cuidardeperto.dto.AuthDTO.UsuarioResponse;
import br.com.fiap.cuidardeperto.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Cadastro e login (publicos)")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Cria conta (cuidador ou responsavel)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Conta criada"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado"),
                    @ApiResponse(responseCode = "422", description = "Papel nao permitido no cadastro publico")})
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrar(req);
    }

    @Operation(summary = "Login: retorna o JWT",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token emitido (use no botao Authorize)"),
                    @ApiResponse(responseCode = "401", description = "Credenciais invalidas")})
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
