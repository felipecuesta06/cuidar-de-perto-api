package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Cuidador;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.IdosoResponsavel;
import br.com.fiap.cuidardeperto.domain.Papel;
import br.com.fiap.cuidardeperto.domain.Responsavel;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.IdosoRequest;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.IdosoResponse;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.LimitesSaude;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.VinculoRequest;
import br.com.fiap.cuidardeperto.dto.IdosoDTO.VinculoResponse;
import br.com.fiap.cuidardeperto.exception.ConflitoException;
import br.com.fiap.cuidardeperto.exception.RecursoNaoEncontradoException;
import br.com.fiap.cuidardeperto.exception.RegraNegocioException;
import br.com.fiap.cuidardeperto.repository.CuidadorRepository;
import br.com.fiap.cuidardeperto.repository.IdosoRepository;
import br.com.fiap.cuidardeperto.repository.IdosoResponsavelRepository;
import br.com.fiap.cuidardeperto.repository.IdosoSpecs;
import br.com.fiap.cuidardeperto.repository.ResponsavelRepository;
import br.com.fiap.cuidardeperto.security.AcessoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdosoService {

    private final IdosoRepository idosoRepository;
    private final CuidadorRepository cuidadorRepository;
    private final ResponsavelRepository responsavelRepository;
    private final IdosoResponsavelRepository vinculoRepository;
    private final AuthService authService;
    private final AcessoService acesso;

    // ------------------------------------------------------------------ CRUD

    /** Cadastra o idoso. Cuidador logado passa a acompanha-lo; admin precisa informar cuidadorId. */
    @Transactional
    public IdosoResponse criar(IdosoRequest req) {
        if (idosoRepository.existsByCpf(req.cpf())) {
            throw new ConflitoException("Ja existe um idoso cadastrado com este CPF");
        }
        Cuidador cuidador = resolverCuidadorResponsavel(req.cuidadorId());

        Idoso idoso = new Idoso();
        aplicar(idoso, req);

        if (req.emailAcesso() != null && !req.emailAcesso().isBlank()) {
            if (req.senhaAcesso() == null || req.senhaAcesso().isBlank()) {
                throw new RegraNegocioException("Informe senhaAcesso para criar o login do idoso");
            }
            idoso.setUsuario(authService.novoUsuario(req.emailAcesso(), req.senhaAcesso(), Papel.IDOSO));
        }
        idoso.getCuidadores().add(cuidador);
        return IdosoResponse.detalhe(idosoRepository.save(idoso));
    }

    @Transactional(readOnly = true)
    public IdosoResponse detalhar(Long id) {
        return IdosoResponse.detalhe(buscar(id));
    }

    /** Busca com filtros dinamicos (Specification); cuidador so enxerga os idosos vinculados a ele. */
    @Transactional(readOnly = true)
    public Page<IdosoResponse> listar(String nome, Integer idadeMin, Integer idadeMax, String condicao,
                                      Boolean ativo, Pageable pageable) {
        Specification<Idoso> spec = Specification.where(IdosoSpecs.nomeContem(nome))
                .and(IdosoSpecs.idadeEntre(idadeMin, idadeMax))
                .and(IdosoSpecs.condicaoContem(condicao))
                .and(IdosoSpecs.ativo(ativo));
        if (acesso.papelAtual() == Papel.CUIDADOR) {
            spec = spec.and(IdosoSpecs.doCuidador(acesso.cuidadorAtual().getId()));
        }
        return idosoRepository.findAll(spec, pageable).map(IdosoResponse::resumo);
    }

    @Transactional
    public IdosoResponse atualizar(Long id, IdosoRequest req) {
        Idoso idoso = buscar(id);
        if (idosoRepository.existsByCpfAndIdNot(req.cpf(), id)) {
            throw new ConflitoException("Ja existe outro idoso cadastrado com este CPF");
        }
        aplicar(idoso, req);
        return IdosoResponse.detalhe(idoso);
    }

    /** Inativacao logica (204): o historico de eventos e alertas e preservado. */
    @Transactional
    public void inativar(Long id) {
        Idoso idoso = buscar(id);
        idoso.setAtivo(false);
        if (idoso.getUsuario() != null) {
            idoso.getUsuario().setAtivo(false);
        }
    }

    // ------------------------------------------------------------------ vinculos

    @Transactional
    public VinculoResponse vincularResponsavel(Long idosoId, Long responsavelId, VinculoRequest req) {
        Idoso idoso = buscar(idosoId);
        Responsavel responsavel = responsavelRepository.findById(responsavelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Responsavel", responsavelId));
        if (!responsavel.isAtivo()) {
            throw new RegraNegocioException("Responsavel inativo nao pode ser vinculado");
        }
        if (vinculoRepository.existsByIdosoIdAndResponsavelId(idosoId, responsavelId)) {
            throw new ConflitoException("Responsavel ja vinculado a este idoso");
        }
        IdosoResponsavel v = new IdosoResponsavel();
        v.setIdoso(idoso);
        v.setResponsavel(responsavel);
        v.setParentesco(req.parentesco().trim());
        v.setPrioridadeContato(req.prioridadeContato());
        return VinculoResponse.de(vinculoRepository.save(v));
    }

    @Transactional
    public void desvincularResponsavel(Long idosoId, Long responsavelId) {
        IdosoResponsavel v = vinculoRepository.findByIdosoIdAndResponsavelId(idosoId, responsavelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo do responsavel " + responsavelId
                        + " com o idoso", idosoId));
        vinculoRepository.delete(v);
    }

    @Transactional(readOnly = true)
    public Page<VinculoResponse> listarResponsaveis(Long idosoId, Pageable pageable) {
        buscar(idosoId);
        return vinculoRepository.findByIdosoId(idosoId, pageable).map(VinculoResponse::de);
    }

    @Transactional
    public void vincularCuidador(Long idosoId, Long cuidadorId) {
        Idoso idoso = buscar(idosoId);
        Cuidador cuidador = cuidadorRepository.findById(cuidadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cuidador", cuidadorId));
        if (!cuidador.isAtivo()) {
            throw new RegraNegocioException("Cuidador inativo nao pode ser vinculado");
        }
        idoso.getCuidadores().add(cuidador);
    }

    @Transactional
    public void desvincularCuidador(Long idosoId, Long cuidadorId) {
        Idoso idoso = buscar(idosoId);
        boolean removido = idoso.getCuidadores().removeIf(c -> c.getId().equals(cuidadorId));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Vinculo do cuidador " + cuidadorId + " com o idoso", idosoId);
        }
    }

    // ------------------------------------------------------------------ internos

    private Idoso buscar(Long id) {
        return idosoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso", id));
    }

    private Cuidador resolverCuidadorResponsavel(Long cuidadorIdInformado) {
        if (acesso.papelAtual() == Papel.CUIDADOR) {
            return acesso.cuidadorAtual();
        }
        if (cuidadorIdInformado == null) {
            throw new RegraNegocioException("Informe cuidadorId: o idoso precisa de ao menos um cuidador");
        }
        return cuidadorRepository.findById(cuidadorIdInformado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cuidador", cuidadorIdInformado));
    }

    private void aplicar(Idoso idoso, IdosoRequest req) {
        idoso.setNome(req.nome().trim());
        idoso.setDataNascimento(req.dataNascimento());
        idoso.setCpf(req.cpf());
        idoso.setTelefone(req.telefone());
        idoso.setEndereco(req.endereco());
        idoso.setCondicoesMedicas(req.condicoesMedicas());

        LimitesSaude l = req.limites();
        if (l != null) {
            idoso.setFcMin(l.fcMin() != null ? l.fcMin() : Idoso.FC_MIN_PADRAO);
            idoso.setFcMax(l.fcMax() != null ? l.fcMax() : Idoso.FC_MAX_PADRAO);
            idoso.setGlicemiaMin(l.glicemiaMin() != null ? l.glicemiaMin() : Idoso.GLICEMIA_MIN_PADRAO);
            idoso.setGlicemiaMax(l.glicemiaMax() != null ? l.glicemiaMax() : Idoso.GLICEMIA_MAX_PADRAO);
            idoso.setSpo2Min(l.spo2Min() != null ? l.spo2Min() : Idoso.SPO2_MIN_PADRAO);
            idoso.setPressaoSistMax(l.pressaoSistMax() != null ? l.pressaoSistMax() : Idoso.PRESSAO_SIST_MAX_PADRAO);
        }
        if (idoso.getFcMin() >= idoso.getFcMax() || idoso.getGlicemiaMin() >= idoso.getGlicemiaMax()) {
            throw new RegraNegocioException("Limites incoerentes: o minimo deve ser menor que o maximo");
        }
    }
}
