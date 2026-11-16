package br.com.fiap.cuidardeperto.dto;

import br.com.fiap.cuidardeperto.domain.Papel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDTO {

    private AuthDTO() {
    }

    @Schema(description = "Cadastro publico de conta. Somente CUIDADOR ou RESPONSAVEL.")
    public record CadastroRequest(
            @Schema(example = "Carla Cuidadora") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "carla@exemplo.com") @NotBlank @Email @Size(max = 150) String email,
            @Schema(example = "Senha@1234") @NotBlank @Size(min = 8, max = 72) String senha,
            @Schema(example = "CUIDADOR") @NotNull Papel papel,
            @Schema(example = "11999990001") @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 digitos") String telefone,
            @Schema(example = "COREN-SP 123456", description = "Apenas para cuidadores") @Size(max = 40) String registroProfissional) {
    }

    public record LoginRequest(
            @Schema(example = "cuidador@cuidar.com") @NotBlank @Email String email,
            @Schema(example = "Cuidador@123") @NotBlank String senha) {
    }

    public record TokenResponse(String token, String tipo, long expiraEmSegundos, Long usuarioId, String email, Papel papel) {
    }

    public record UsuarioResponse(Long id, String email, Papel papel, Long perfilId) {
    }
}
