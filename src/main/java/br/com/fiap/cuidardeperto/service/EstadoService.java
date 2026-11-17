package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.config.CacheConfig;
import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import br.com.fiap.cuidardeperto.domain.TipoSinal;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.AlertaResponse;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EstadoIdosoResponse;
import br.com.fiap.cuidardeperto.dto.SensorDTO.SinalEstado;
import br.com.fiap.cuidardeperto.exception.RecursoNaoEncontradoException;
import br.com.fiap.cuidardeperto.repository.AlertaRepository;
import br.com.fiap.cuidardeperto.repository.EventoSensorRepository;
import br.com.fiap.cuidardeperto.repository.IdosoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadoService {

    private final IdosoRepository idosoRepository;
    private final EventoSensorRepository eventoRepository;
    private final AlertaRepository alertaRepository;


    @Cacheable(cacheNames = CacheConfig.IDOSO_ESTADO, key = "#idosoId")
    @Transactional(readOnly = true)
    public EstadoIdosoResponse estadoDoIdoso(Long idosoId) {
        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso", idosoId));

        List<SinalEstado> sinais = new ArrayList<>();
        for (TipoSinal tipo : TipoSinal.values()) {
            eventoRepository.findFirstByIdosoIdAndTipoOrderByDataHoraDesc(idosoId, tipo)
                    .ifPresent(e -> sinais.add(new SinalEstado(tipo, e.getValor(), e.getUnidade(),
                            e.getDataHora(), e.getGravidade())));
        }

        List<AlertaResponse> abertos = alertaRepository
                .findByIdosoIdAndStatusIn(idosoId, List.of(StatusAlerta.ABERTO, StatusAlerta.EM_ATENDIMENTO))
                .stream().map(AlertaResponse::de).toList();

        Gravidade pior = abertos.stream().map(AlertaResponse::gravidade)
                .max(Comparator.naturalOrder()).orElse(null);

        return new EstadoIdosoResponse(idosoId, idoso.getNome(), LocalDateTime.now(), sinais, abertos, pior);
    }
}
