package br.com.fiap.cuidardeperto.dto;

import br.com.fiap.cuidardeperto.domain.EventoSensor;
import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.TipoSinal;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class SensorDTO {

    private SensorDTO() {
    }

    @Schema(description = "Leitura de sensor. Para QUEDA use valor 1 (detectada) ou 2 (sem resposta); BOTAO_PANICO dispensa valor.")
    public record EventoSensorRequest(
            @Schema(example = "1") @NotNull Long idosoId,
            @Schema(example = "BATIMENTO") @NotNull TipoSinal tipo,
            @Schema(example = "145") Double valor,
            @Schema(example = "bpm", description = "Opcional: assume a unidade padrao do sinal") @Size(max = 20) String unidade,
            @Schema(example = "pulseira-001") @Size(max = 80) String dispositivo,
            @Schema(description = "Opcional: assume o horario atual") LocalDateTime dataHora) {
    }

    public record EventoSensorResponse(Long id, Long idosoId, TipoSinal tipo, Double valor, String unidade,
                                       String dispositivo, LocalDateTime dataHora, Gravidade gravidade,
                                       Long alertaId) {
        public static EventoSensorResponse de(EventoSensor e, Long alertaId) {
            return new EventoSensorResponse(e.getId(), e.getIdoso().getId(), e.getTipo(), e.getValor(),
                    e.getUnidade(), e.getDispositivo(), e.getDataHora(), e.getGravidade(), alertaId);
        }
    }

    public record SinalEstado(TipoSinal tipo, Double valor, String unidade, LocalDateTime dataHora, Gravidade gravidade) {
    }

    public record EstadoIdosoResponse(Long idosoId, String nome, LocalDateTime consultadoEm,
                                      List<SinalEstado> sinais, List<AlertaDTO.AlertaResponse> alertasAbertos,
                                      Gravidade piorGravidadeAberta) {
    }
}
