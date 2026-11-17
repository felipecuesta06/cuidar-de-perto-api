package br.com.fiap.cuidardeperto.dto;

import br.com.fiap.cuidardeperto.domain.Frequencia;
import br.com.fiap.cuidardeperto.domain.LembreteRemedio;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class LembreteDTO {

    private LembreteDTO() {
    }

    public record LembreteRequest(
            @Schema(example = "Losartana") @NotBlank @Size(max = 120) String medicamento,
            @Schema(example = "50 mg") @NotBlank @Size(max = 80) String dosagem,
            @Schema(example = "08:00") @NotNull LocalTime horario,
            @Schema(example = "DIARIA") @NotNull Frequencia frequencia,
            @Schema(example = "2026-11-13") @NotNull LocalDate inicio,
            @Schema(example = "2026-12-31", description = "Opcional") LocalDate fim,
            @Schema(example = "true", description = "Padrao: true") Boolean ativo) {

        @JsonIgnore
        @AssertTrue(message = "fim deve ser igual ou posterior a inicio")
        public boolean isPeriodoCoerente() {
            return fim == null || inicio == null || !fim.isBefore(inicio);
        }
    }

    public record LembreteResponse(Long id, Long idosoId, String medicamento, String dosagem, LocalTime horario,
                                   Frequencia frequencia, LocalDate inicio, LocalDate fim, boolean ativo,
                                   LocalDateTime ultimaConfirmacao, boolean confirmadoHoje) {
        public static LembreteResponse de(LembreteRemedio l) {
            return new LembreteResponse(l.getId(), l.getIdoso().getId(), l.getMedicamento(), l.getDosagem(),
                    l.getHorario(), l.getFrequencia(), l.getInicio(), l.getFim(), l.isAtivo(),
                    l.getUltimaConfirmacao(), l.confirmadoEm(LocalDate.now()));
        }
    }
}
