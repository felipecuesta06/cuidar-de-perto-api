package br.com.fiap.cuidardeperto.dto;

import br.com.fiap.cuidardeperto.domain.Cuidador;
import br.com.fiap.cuidardeperto.domain.Responsavel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public final class PessoaDTO {

    private PessoaDTO() {
    }

    public record CuidadorRequest(
            @Schema(example = "Carla Cuidadora") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "11999990001") @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 digitos") String telefone,
            @Schema(example = "COREN-SP 123456") @Size(max = 40) String registroProfissional) {
    }

    public record CuidadorResponse(Long id, String nome, String email, String telefone,
                                   String registroProfissional, boolean ativo) {
        public static CuidadorResponse de(Cuidador c) {
            return new CuidadorResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone(),
                    c.getRegistroProfissional(), c.isAtivo());
        }
    }

    public record ResponsavelRequest(
            @Schema(example = "Rafael Responsavel") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "11999990002") @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 digitos") String telefone) {
    }

    public record ResponsavelResponse(Long id, String nome, String email, String telefone, boolean ativo) {
        public static ResponsavelResponse de(Responsavel r) {
            return new ResponsavelResponse(r.getId(), r.getNome(), r.getEmail(), r.getTelefone(), r.isAtivo());
        }
    }
}
