package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Alerta;
import br.com.fiap.cuidardeperto.domain.Cuidador;
import br.com.fiap.cuidardeperto.domain.EventoSensor;
import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.IdosoResponsavel;
import br.com.fiap.cuidardeperto.domain.Papel;
import br.com.fiap.cuidardeperto.domain.StatusAlerta;
import br.com.fiap.cuidardeperto.domain.TipoAlerta;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.AlertaResponse;
import br.com.fiap.cuidardeperto.dto.AlertaDTO.ResumoGravidadeResponse;
import br.com.fiap.cuidardeperto.exception.RecursoNaoEncontradoException;
import br.com.fiap.cuidardeperto.exception.RegraNegocioException;
import br.com.fiap.cuidardeperto.repository.AlertaRepository;
import br.com.fiap.cuidardeperto.repository.AlertaSpecs;
import br.com.fiap.cuidardeperto.repository.IdosoResponsavelRepository;
import br.com.fiap.cuidardeperto.security.AcessoService;
import br.com.fiap.cuidardeperto.service.NotificacaoService.Destinatario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regras de alerta: anti-duplicidade, escalonamento por gravidade, notificacao e resolucao.
 * Os caches idoso-estado e painel-cuidador sao invalidados sempre que um alerta e criado ou alterado.
 */
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final IdosoResponsavelRepository vinculoRepository;
    private final NotificacaoService notificacaoService;
    private final AcessoService acesso;

    // ------------------------------------------------------------------ criacao / agregacao

    /**
     * Abre um alerta ou, se ja existe um ABERTO do mesmo tipo para o mesmo idoso, agrega a ocorrencia a ele
     * (regra anti-duplicidade). A gravidade nunca diminui na agregacao.
     */
    @Transactional
    public Alerta abrirOuAgregar(Idoso idoso, EventoSensor evento, TipoAlerta tipo, Gravidade gravidade,
                                 String mensagem) {
        Optional<Alerta> existente = alertaRepository
                .findFirstByIdosoIdAndTipoAndStatus(idoso.getId(), tipo, StatusAlerta.ABERTO);

        Alerta alerta;
        if (existente.isPresent()) {
            alerta = existente.get();
            alerta.setOcorrencias(alerta.getOcorrencias() + 1);
            alerta.setMensagem(mensagem);
            if (gravidade.ordinal() > alerta.getGravidade().ordinal()) {
                alerta.setGravidade(gravidade);
            }
        } else {
            alerta = new Alerta();
            alerta.setIdoso(idoso);
            alerta.setEvento(evento);
            alerta.setTipo(tipo);
            alerta.setGravidade(gravidade);
            alerta.setMensagem(mensagem);
            alerta.setStatus(StatusAlerta.ABERTO);
            alerta.setCriadoEm(LocalDateTime.now());
            alerta = alertaRepository.save(alerta);
        }
        notificar(alerta, idoso);
        return alerta;
    }

    // ------------------------------------------------------------------ notificacao

    /**
     * BAIXA/MEDIA: cuidadores. ALTA: cuidadores + responsavel de maior prioridade. CRITICA: cuidadores + TODOS os
     * responsaveis. Quem ja foi notificado neste alerta nao e avisado de novo.
     */
    private void notificar(Alerta alerta, Idoso idoso) {
        List<Destinatario> alvo = new ArrayList<>();
        for (Cuidador c : idoso.getCuidadores()) {
            if (c.isAtivo()) {
                alvo.add(new Destinatario("CUIDADOR", c.getNome(), c.getEmail(), c.getTelefone()));
            }
        }
        List<IdosoResponsavel> vinculos = responsaveisAtivos(idoso.getId());
        if (alerta.getGravidade() == Gravidade.CRITICA) {
            vinculos.forEach(v -> alvo.add(paraDestinatario(v)));
        } else if (alerta.getGravidade() == Gravidade.ALTA && !vinculos.isEmpty()) {
            alvo.add(paraDestinatario(vinculos.get(0)));
        }
        enviar(alerta, alvo);
    }

    private void notificarTodosResponsaveis(Alerta alerta) {
        List<Destinatario> alvo = new ArrayList<>();
        responsaveisAtivos(alerta.getIdoso().getId()).forEach(v -> alvo.add(paraDestinatario(v)));
        enviar(alerta, alvo);
    }

    private List<IdosoResponsavel> responsaveisAtivos(Long idosoId) {
        return vinculoRepository.findComResponsavelByIdosoId(idosoId).stream()
                .filter(v -> v.getResponsavel().isAtivo())
                .toList();
    }

    private Destinatario paraDestinatario(IdosoResponsavel v) {
        return new Destinatario("RESPONSAVEL (" + v.getParentesco() + ")", v.getResponsavel().getNome(),
                v.getResponsavel().getEmail(), v.getResponsavel().getTelefone());
    }

    private void enviar(Alerta alerta, List<Destinatario> destinatarios) {
        for (Destinatario d : destinatarios) {
            if (!alerta.jaNotificado(d.descricao())) {
                notificacaoService.notificar(alerta, d);
                alerta.registrarNotificado(d.descricao());
            }
        }
    }

    // ------------------------------------------------------------------ escalonamento automatico

    /**
     * Chamado pelo agendador:
     * (1) queda ALTA sem atendimento ha 2 min vira CRITICA ("queda sem resposta");
     * (2) alerta ALTA aberto ha 5 min e escalado para todos os responsaveis.
     */
    @Transactional
    public int escalarPendentes(LocalDateTime agora) {
        int total = 0;

        List<Alerta> quedas = alertaRepository.findByTipoAndGravidadeAndStatusAndCriadoEmBefore(
                TipoAlerta.QUEDA, Gravidade.ALTA, StatusAlerta.ABERTO, agora.minusMinutes(2));
        for (Alerta a : quedas) {
            a.setGravidade(Gravidade.CRITICA);
            a.setMensagem("Queda sem resposta em 2 minutos");
            a.setEscalonado(true);
            notificar(a, a.getIdoso());
            total++;
        }

        List<Alerta> altas = alertaRepository.findByGravidadeAndStatusAndEscalonadoFalseAndCriadoEmBefore(
                Gravidade.ALTA, StatusAlerta.ABERTO, agora.minusMinutes(5));
        for (Alerta a : altas) {
            a.setEscalonado(true);
            notificarTodosResponsaveis(a);
            total++;
        }
        return total;
    }

    // ------------------------------------------------------------------ resolucao

    /** So cuidador vinculado ou admin; a resolucao exige observacao. */
    @Transactional
    public AlertaResponse atualizarStatus(Long id, StatusAlerta novo, String observacao) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta", id));
        acesso.exigirEditar(alerta.getIdoso().getId());

        if (novo == StatusAlerta.ABERTO) {
            throw new RegraNegocioException("O status de destino deve ser EM_ATENDIMENTO ou RESOLVIDO");
        }
        if (alerta.getStatus() == StatusAlerta.RESOLVIDO) {
            throw new RegraNegocioException("Alerta ja resolvido nao pode ser alterado");
        }
        if (alerta.getStatus() == novo) {
            throw new RegraNegocioException("O alerta ja esta em " + novo);
        }
        boolean temObservacao = observacao != null && !observacao.isBlank();
        if (novo == StatusAlerta.RESOLVIDO && !temObservacao) {
            throw new RegraNegocioException("A resolucao do alerta exige uma observacao");
        }

        alerta.setStatus(novo);
        if (temObservacao) {
            alerta.setObservacao(observacao.trim());
        }
        if (novo == StatusAlerta.RESOLVIDO) {
            alerta.setResolvidoEm(LocalDateTime.now());
        }
        return AlertaResponse.de(alerta);
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public AlertaResponse buscar(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta", id));
        acesso.exigirVer(alerta.getIdoso().getId());
        return AlertaResponse.de(alerta);
    }

    /** Lista com filtros dinamicos (Specification), restrita ao escopo de posse do usuario. */
    @Transactional(readOnly = true)
    public Page<AlertaResponse> listar(Long idosoId, Gravidade gravidade, StatusAlerta status,
                                       LocalDateTime de, LocalDateTime ate, Pageable pageable) {
        Specification<Alerta> spec = Specification.where(AlertaSpecs.idoso(idosoId))
                .and(AlertaSpecs.gravidade(gravidade))
                .and(AlertaSpecs.status(status))
                .and(AlertaSpecs.periodo(de, ate));

        Papel papel = acesso.papelAtual();
        if (papel == Papel.CUIDADOR) {
            spec = spec.and(AlertaSpecs.doCuidador(acesso.cuidadorAtual().getId()));
        } else if (papel == Papel.RESPONSAVEL) {
            spec = spec.and(AlertaSpecs.doResponsavel(acesso.responsavelAtual().getId()));
        }
        return alertaRepository.findAll(spec, pageable).map(AlertaResponse::de);
    }

    /**
     * Painel do cuidador (consulta critica). Cache "painel-cuidador": e a tela mais aberta e tem agregacao pesada;
     * invalidado quando qualquer alerta e criado/alterado.
     */
    @Transactional(readOnly = true)
    public Page<AlertaResponse> painelUrgente(Long cuidadorId, Pageable pageable) {
        return alertaRepository.painelUrgente(cuidadorId, pageable).map(AlertaResponse::de);
    }

    @Transactional(readOnly = true)
    public List<ResumoGravidadeResponse> resumoUltimas24h() {
        return alertaRepository.contagemPorGravidade(LocalDateTime.now().minusHours(24)).stream()
                .map(r -> new ResumoGravidadeResponse(r.getGravidade(), r.getTotal()))
                .toList();
    }
}
