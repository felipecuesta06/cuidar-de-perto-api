package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.LembreteRemedio;
import br.com.fiap.cuidardeperto.domain.TipoAlerta;
import br.com.fiap.cuidardeperto.dto.LembreteDTO.LembreteRequest;
import br.com.fiap.cuidardeperto.dto.LembreteDTO.LembreteResponse;
import br.com.fiap.cuidardeperto.exception.RecursoNaoEncontradoException;
import br.com.fiap.cuidardeperto.repository.IdosoRepository;
import br.com.fiap.cuidardeperto.repository.LembreteRepository;
import br.com.fiap.cuidardeperto.security.AcessoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LembreteService {

    private static final int TOLERANCIA_MINUTOS = 30;
    private static final int OMISSOES_PARA_ALTA = 3;

    private final LembreteRepository lembreteRepository;
    private final IdosoRepository idosoRepository;
    private final AlertaService alertaService;
    private final AcessoService acesso;

    @Transactional
    public LembreteResponse criar(Long idosoId, LembreteRequest req) {
        Idoso idoso = idosoRepository.findById(idosoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso", idosoId));
        LembreteRemedio lembrete = new LembreteRemedio();
        lembrete.setIdoso(idoso);
        preencher(lembrete, req);
        return LembreteResponse.de(lembreteRepository.save(lembrete));
    }

    /** Lembretes ativos vigentes hoje. Cache "lembretes-dia" (chave = idosoId): dado muda pouco durante o dia. */
    @Transactional(readOnly = true)
    public List<LembreteResponse> ativosDoDia(Long idosoId) {
        LocalDate hoje = LocalDate.now();
        return lembreteRepository.findByIdosoIdAndAtivoTrue(idosoId).stream()
                .filter(l -> l.aplicaEm(hoje))
                .sorted(Comparator.comparing(LembreteRemedio::getHorario))
                .map(LembreteResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LembreteResponse> todos(Long idosoId, Pageable pageable) {
        return lembreteRepository.findByIdosoId(idosoId, pageable).map(LembreteResponse::de);
    }

    @Transactional
    public LembreteResponse atualizar(Long id, LembreteRequest req) {
        LembreteRemedio lembrete = buscar(id);
        acesso.exigirEditar(lembrete.getIdoso().getId());
        preencher(lembrete, req);
        return LembreteResponse.de(lembrete);
    }

    @Transactional
    public void remover(Long id) {
        LembreteRemedio lembrete = buscar(id);
        acesso.exigirEditar(lembrete.getIdoso().getId());
        lembreteRepository.delete(lembrete);
    }

    /** Registra a tomada: zera as omissoes seguidas. */
    @Transactional
    public LembreteResponse confirmar(Long id) {
        LembreteRemedio lembrete = buscar(id);
        acesso.exigirRegistrar(lembrete.getIdoso().getId());
        lembrete.setUltimaConfirmacao(LocalDateTime.now());
        lembrete.setOmissoesSeguidas(0);
        return LembreteResponse.de(lembrete);
    }

    /**
     * Chamado pelo agendador. Se o idoso nao confirmou a tomada ate 30 min depois do horario, gera alerta MEDIA ao
     * cuidador; a terceira omissao seguida do mesmo remedio sobe para ALTA. Cada lembrete conta no maximo uma
     * omissao por dia.
     */
    @Transactional
    public int verificarOmissoes(LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();
        int geradas = 0;
        for (LembreteRemedio l : lembreteRepository.findAtivosComIdoso()) {
            if (!l.aplicaEm(hoje)) {
                continue;
            }
            LocalDateTime horarioHoje = LocalDateTime.of(hoje, l.getHorario());
            if (agora.isBefore(horarioHoje.plusMinutes(TOLERANCIA_MINUTOS))
                    || l.getCriadoEm().isAfter(horarioHoje)
                    || l.confirmadoEm(hoje)
                    || hoje.equals(l.getUltimaVerificacao())) {
                continue;
            }
            l.setUltimaVerificacao(hoje);
            l.setOmissoesSeguidas(l.getOmissoesSeguidas() + 1);
            Gravidade gravidade = l.getOmissoesSeguidas() >= OMISSOES_PARA_ALTA ? Gravidade.ALTA : Gravidade.MEDIA;
            String mensagem = l.getIdoso().getNome() + " nao confirmou " + l.getMedicamento() + " (" + l.getDosagem()
                    + ") das " + l.getHorario().format(DateTimeFormatter.ofPattern("HH:mm"))
                    + " - omissoes seguidas: " + l.getOmissoesSeguidas();
            alertaService.abrirOuAgregar(l.getIdoso(), null, TipoAlerta.LEMBRETE_NAO_CONFIRMADO, gravidade, mensagem);
            geradas++;
        }
        return geradas;
    }

    private LembreteRemedio buscar(Long id) {
        return lembreteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lembrete", id));
    }

    private void preencher(LembreteRemedio l, LembreteRequest req) {
        l.setMedicamento(req.medicamento());
        l.setDosagem(req.dosagem());
        l.setHorario(req.horario());
        l.setFrequencia(req.frequencia());
        l.setInicio(req.inicio());
        l.setFim(req.fim());
        l.setAtivo(req.ativo() == null || req.ativo());
    }
}
