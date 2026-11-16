package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Cuidador;
import br.com.fiap.cuidardeperto.domain.Responsavel;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.CuidadorRequest;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.CuidadorResponse;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.ResponsavelRequest;
import br.com.fiap.cuidardeperto.dto.PessoaDTO.ResponsavelResponse;
import br.com.fiap.cuidardeperto.exception.RecursoNaoEncontradoException;
import br.com.fiap.cuidardeperto.repository.CuidadorRepository;
import br.com.fiap.cuidardeperto.repository.ResponsavelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manutencao de cuidadores e responsaveis (a exclusao e logica: ativo = false, a conta de acesso e bloqueada). */
@Service
@RequiredArgsConstructor
public class PessoaService {

    private final CuidadorRepository cuidadorRepository;
    private final ResponsavelRepository responsavelRepository;

    // ---- cuidadores
    @Transactional(readOnly = true)
    public Page<CuidadorResponse> listarCuidadores(Pageable pageable) {
        return cuidadorRepository.findAll(pageable).map(CuidadorResponse::de);
    }

    @Transactional(readOnly = true)
    public CuidadorResponse buscarCuidador(Long id) {
        return CuidadorResponse.de(cuidador(id));
    }

    @Transactional
    public CuidadorResponse atualizarCuidador(Long id, CuidadorRequest req) {
        Cuidador c = cuidador(id);
        c.setNome(req.nome().trim());
        c.setTelefone(req.telefone());
        c.setRegistroProfissional(req.registroProfissional());
        return CuidadorResponse.de(c);
    }

    @Transactional
    public void inativarCuidador(Long id) {
        Cuidador c = cuidador(id);
        c.setAtivo(false);
        c.getUsuario().setAtivo(false);
    }

    // ---- responsaveis
    @Transactional(readOnly = true)
    public Page<ResponsavelResponse> listarResponsaveis(Pageable pageable) {
        return responsavelRepository.findAll(pageable).map(ResponsavelResponse::de);
    }

    @Transactional(readOnly = true)
    public ResponsavelResponse buscarResponsavel(Long id) {
        return ResponsavelResponse.de(responsavel(id));
    }

    @Transactional
    public ResponsavelResponse atualizarResponsavel(Long id, ResponsavelRequest req) {
        Responsavel r = responsavel(id);
        r.setNome(req.nome().trim());
        r.setTelefone(req.telefone());
        return ResponsavelResponse.de(r);
    }

    @Transactional
    public void inativarResponsavel(Long id) {
        Responsavel r = responsavel(id);
        r.setAtivo(false);
        r.getUsuario().setAtivo(false);
    }

    private Cuidador cuidador(Long id) {
        return cuidadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cuidador", id));
    }

    private Responsavel responsavel(Long id) {
        return responsavelRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Responsavel", id));
    }
}
