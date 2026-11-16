package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Alerta;
import br.com.fiap.cuidardeperto.domain.EventoSensor;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.TipoSinal;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EventoSensorRequest;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EventoSensorResponse;
import br.com.fiap.cuidardeperto.exception.RecursoNaoEncontradoException;
import br.com.fiap.cuidardeperto.exception.RegraNegocioException;
import br.com.fiap.cuidardeperto.repository.EventoSensorRepository;
import br.com.fiap.cuidardeperto.repository.IdosoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventoSensorService {

    private final EventoSensorRepository eventoRepository;
    private final IdosoRepository idosoRepository;
    private final ClassificadorSaude classificador;
    private final AlertaService alertaService;

    /**
     * Recebe a leitura, classifica contra os limites do idoso e, se estiver fora do ideal, abre/agrega um alerta.
     * Nao e cacheado, mas invalida o estado do idoso (a leitura mais recente mudou).
     */
    @Transactional
    public EventoSensorResponse registrar(EventoSensorRequest req) {
        Idoso idoso = idosoRepository.findById(req.idosoId())
                .filter(Idoso::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso ativo", req.idosoId()));

        double valor = resolverValor(req);

        EventoSensor evento = new EventoSensor();
        evento.setIdoso(idoso);
        evento.setTipo(req.tipo());
        evento.setValor(valor);
        evento.setUnidade(req.unidade() != null && !req.unidade().isBlank() ? req.unidade() : unidadePadrao(req.tipo()));
        evento.setDispositivo(req.dispositivo());
        evento.setDataHora(req.dataHora() != null ? req.dataHora() : LocalDateTime.now());

        ClassificadorSaude.Resultado resultado = classificador.classificar(idoso, req.tipo(), valor);
        evento.setGravidade(resultado.gravidade());
        evento = eventoRepository.save(evento);

        Long alertaId = null;
        if (!resultado.normal()) {
            Alerta alerta = alertaService.abrirOuAgregar(idoso, evento, resultado.tipoAlerta(),
                    resultado.gravidade(), resultado.mensagem());
            alertaId = alerta.getId();
        }
        return EventoSensorResponse.de(evento, alertaId);
    }

    /** Historico com filtro opcional por tipo e periodo (ultimos 30 dias por padrao). */
    @Transactional(readOnly = true)
    public Page<EventoSensorResponse> historico(Long idosoId, TipoSinal tipo, LocalDateTime de, LocalDateTime ate,
                                                Pageable pageable) {
        LocalDateTime desde = de != null ? de : LocalDateTime.now().minusDays(30);
        LocalDateTime ateQuando = ate != null ? ate : LocalDateTime.now().plusDays(1);
        Page<EventoSensor> pagina = tipo != null
                ? eventoRepository.historicoPorTipo(idosoId, tipo, desde, ateQuando, pageable)
                : eventoRepository.historico(idosoId, desde, ateQuando, pageable);
        return pagina.map(e -> EventoSensorResponse.de(e, null));
    }

    private double resolverValor(EventoSensorRequest req) {
        if (req.valor() != null) {
            return req.valor();
        }
        if (req.tipo() == TipoSinal.QUEDA || req.tipo() == TipoSinal.BOTAO_PANICO) {
            return 1.0;
        }
        throw new RegraNegocioException("O campo valor e obrigatorio para o sinal " + req.tipo());
    }

    private String unidadePadrao(TipoSinal tipo) {
        return switch (tipo) {
            case BATIMENTO -> "bpm";
            case GLICEMIA -> "mg/dL";
            case SPO2 -> "%";
            case PRESSAO -> "mmHg";
            case QUEDA, BOTAO_PANICO -> "evento";
        };
    }
}
