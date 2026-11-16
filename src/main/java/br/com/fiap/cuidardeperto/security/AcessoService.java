package br.com.fiap.cuidardeperto.security;

import br.com.fiap.cuidardeperto.domain.Cuidador;
import br.com.fiap.cuidardeperto.domain.Papel;
import br.com.fiap.cuidardeperto.domain.Responsavel;
import br.com.fiap.cuidardeperto.repository.CuidadorRepository;
import br.com.fiap.cuidardeperto.repository.IdosoRepository;
import br.com.fiap.cuidardeperto.repository.IdosoResponsavelRepository;
import br.com.fiap.cuidardeperto.repository.ResponsavelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


@Service("acesso")
@RequiredArgsConstructor
public class AcessoService {

    private final IdosoRepository idosoRepository;
    private final CuidadorRepository cuidadorRepository;
    private final ResponsavelRepository responsavelRepository;
    private final IdosoResponsavelRepository vinculoRepository;

    public UsuarioPrincipal usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Usuario nao autenticado");
    }

    public Papel papelAtual() {
        return usuarioAtual().getPapel();
    }

    public Cuidador cuidadorAtual() {
        return cuidadorRepository.findByUsuarioId(usuarioAtual().getId())
                .orElseThrow(() -> new AccessDeniedException("Perfil de cuidador nao encontrado"));
    }

    public Responsavel responsavelAtual() {
        return responsavelRepository.findByUsuarioId(usuarioAtual().getId())
                .orElseThrow(() -> new AccessDeniedException("Perfil de responsavel nao encontrado"));
    }


    public boolean podeVer(Long idosoId) {
        UsuarioPrincipal u = usuarioAtual();
        return switch (u.getPapel()) {
            case ADMIN -> true;
            case CUIDADOR -> idosoRepository.cuidadorVinculado(idosoId, u.getId());
            case RESPONSAVEL -> vinculoRepository.responsavelVinculado(idosoId, u.getId());
            case IDOSO -> idosoRepository.ehOProprioIdoso(idosoId, u.getId());
        };
    }


    public boolean podeEditar(Long idosoId) {
        UsuarioPrincipal u = usuarioAtual();
        return switch (u.getPapel()) {
            case ADMIN -> true;
            case CUIDADOR -> idosoRepository.cuidadorVinculado(idosoId, u.getId());
            default -> false;
        };
    }


    public boolean podeRegistrar(Long idosoId) {
        UsuarioPrincipal u = usuarioAtual();
        if (u.getPapel() == Papel.IDOSO) {
            return idosoRepository.ehOProprioIdoso(idosoId, u.getId());
        }
        return podeEditar(idosoId);
    }


    public boolean ehOIdoso(Long idosoId) {
        UsuarioPrincipal u = usuarioAtual();
        return u.getPapel() == Papel.IDOSO && idosoRepository.ehOProprioIdoso(idosoId, u.getId());
    }

    public boolean ehOCuidador(Long cuidadorId) {
        return cuidadorRepository.pertenceAoUsuario(cuidadorId, usuarioAtual().getId());
    }

    public boolean ehOResponsavel(Long responsavelId) {
        return responsavelRepository.pertenceAoUsuario(responsavelId, usuarioAtual().getId());
    }

    public void exigirVer(Long idosoId) {
        if (!podeVer(idosoId)) {
            throw new AccessDeniedException("Sem vinculo com o idoso " + idosoId);
        }
    }

    public void exigirEditar(Long idosoId) {
        if (!podeEditar(idosoId)) {
            throw new AccessDeniedException("Sem permissao de edicao sobre o idoso " + idosoId);
        }
    }

    public void exigirRegistrar(Long idosoId) {
        if (!podeRegistrar(idosoId)) {
            throw new AccessDeniedException("Sem permissao para registrar dados do idoso " + idosoId);
        }
    }
}
