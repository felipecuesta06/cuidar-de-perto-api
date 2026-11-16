package br.com.fiap.cuidardeperto.dto;

import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.IdosoResponsavel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class IdosoDTO {

    private IdosoDTO() {
    }


    public record LimitesSaude(
            @Schema(example = "60") @Positive Integer fcMin,
            @Schema(example = "100") @Positive Integer fcMax,
            @Schema(example = "70") @Positive Integer glicemiaMin,
            @Schema(example = "140") @Positive Integer glicemiaMax,
            @Schema(example = "95") @Positive @Max(100) Integer spo2Min,
            @Schema(example = "140") @Positive Integer pressaoSistMax) {

        @JsonIgnore
        @AssertTrue(message = "fcMin deve ser menor que fcMax")
        public boolean isFcCoerente() {
            return fcMin == null || fcMax == null || fcMin < fcMax;
        }

        @JsonIgnore
        @AssertTrue(message = "glicemiaMin deve ser menor que glicemiaMax")
        public boolean isGlicemiaCoerente() {
            return glicemiaMin == null || glicemiaMax == null || glicemiaMin < glicemiaMax;
        }

        public static LimitesSaude de(Idoso i) {
            return new LimitesSaude(i.getFcMin(), i.getFcMax(), i.getGlicemiaMin(), i.getGlicemiaMax(),
                    i.getSpo2Min(), i.getPressaoSistMax());
        }
    }

    public record IdosoRequest(
            @Schema(example = "Dona Maria Aparecida") @NotBlank @Size(max = 120) String nome,
            @Schema(example = "1945-03-12") @NotNull @Past LocalDate dataNascimento,
            @Schema(example = "12345678909", description = "11 digitos, sem pontuacao") @NotBlank
            @Pattern(regexp = "\\d{11}", message = "cpf deve ter 11 digitos") String cpf,
            @Schema(example = "11999990003") @Pattern(regexp = "\\d{10,11}", message = "telefone deve ter 10 ou 11 digitos") String telefone,
            @Schema(example = "Rua das Flores, 100 - Sao Paulo/SP") @Size(max = 250) String endereco,
            @Schema(example = "Hipertensao e diabetes tipo 2") @Size(max = 1000) String condicoesMedicas,
            @Valid LimitesSaude limites,
            @Schema(description = "Opcional: cria o login do proprio idoso (papel IDOSO)", example = "dona.maria@exemplo.com")
            @Email @Size(max = 150) String emailAcesso,
            @Schema(description = "Obrigatoria quando emailAcesso for informado") @Size(min = 8, max = 72) String senhaAcesso,
            @Schema(description = "Somente ADMIN: cuidador que passara a acompanhar o idoso") Long cuidadorId) {
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdosoResponse(Long id, String nome, LocalDate dataNascimento, Integer idade, boolean ativo,
                                String cpfMascarado, String telefone, String endereco,
                                String condicoesMedicas, LimitesSaude limites) {

        public static IdosoResponse resumo(Idoso i) {
            return new IdosoResponse(i.getId(), i.getNome(), i.getDataNascimento(), i.idade(), i.isAtivo(),
                    null, null, null, null, null);
        }

        public static IdosoResponse detalhe(Idoso i) {
            return new IdosoResponse(i.getId(), i.getNome(), i.getDataNascimento(), i.idade(), i.isAtivo(),
                    mascarar(i.getCpf()), i.getTelefone(), i.getEndereco(), i.getCondicoesMedicas(),
                    LimitesSaude.de(i));
        }

        private static String mascarar(String cpf) {
            return cpf == null || cpf.length() < 2 ? null : "***.***.***-" + cpf.substring(cpf.length() - 2);
        }
    }

    public record VinculoRequest(
            @Schema(example = "FILHO") @NotBlank @Size(max = 40) String parentesco,
            @Schema(example = "1", description = "1 = maior prioridade de contato") @NotNull @Min(1) @Max(10) Integer prioridadeContato) {
    }

    public record VinculoResponse(Long idosoId, Long responsavelId, String nome, String email, String telefone,
                                  String parentesco, Integer prioridadeContato) {
        public static VinculoResponse de(IdosoResponsavel v) {
            return new VinculoResponse(v.getIdoso().getId(), v.getResponsavel().getId(), v.getResponsavel().getNome(),
                    v.getResponsavel().getEmail(), v.getResponsavel().getTelefone(), v.getParentesco(),
                    v.getPrioridadeContato());
        }
    }
}
