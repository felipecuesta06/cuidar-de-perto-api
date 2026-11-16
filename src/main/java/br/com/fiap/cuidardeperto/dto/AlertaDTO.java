package br.com.fiap.cuidardeperto.dto;

import br.com.fiap.cuidardeperto.domain.Alerta;
import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import br.com.fiap.cuidardeperto.domain.TipoAlerta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AlertaDTO {

    private AlertaDTO() {
    }

    public record AlertaResponse(Long id, Long idosoId, String idosoNome, Long eventoId, TipoAlerta tipo,
                                 Gravidade gravidade, String mensagem, StatusAlerta status,
                                 LocalDateTime criadoEm, LocalDateTime resolvidoEm, List<String> notificados,
                                 String observacao, int ocorrencias, boolean escalonado) {
        public static AlertaResponse de(Alerta a) {
            return new AlertaResponse(a.getId(), a.getIdoso().getId(), a.getIdoso().getNome(),
                    a.getEvento() == null ? null : a.getEvento().getId(), a.getTipo(), a.getGravidade(),
                    a.getMensagem(), a.getStatus(), a.getCriadoEm(), a.getResolvidoEm(), a.notificadosLista(),
                    a.getObservacao(), a.getOcorrencias(), a.isEscalonado());
        }
    }

    public record AlertaStatusRequest(
            @Schema(example = "RESOLVIDO", description = "EM_ATENDIMENTO ou RESOLVIDO") @NotNull StatusAlerta status,
            @Schema(example = "Contato feito por telefone; pressao normalizada.",
                    description = "Obrigatoria ao resolver") @Size(max = 500) String observacao) {
    }

    public record ResumoGravidadeResponse(Gravidade gravidade, long total) {
    }
}
