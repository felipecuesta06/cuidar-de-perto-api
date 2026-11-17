package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Cuidador;
import br.com.fiap.cuidardeperto.domain.Papel;
import br.com.fiap.cuidardeperto.domain.Responsavel;
import br.com.fiap.cuidardeperto.domain.Usuario;
import br.com.fiap.cuidardeperto.dto.AuthDTO.CadastroRequest;
import br.com.fiap.cuidardeperto.dto.AuthDTO.LoginRequest;
import br.com.fiap.cuidardeperto.dto.AuthDTO.TokenResponse;
import br.com.fiap.cuidardeperto.dto.AuthDTO.UsuarioResponse;
import br.com.fiap.cuidardeperto.exception.ConflitoException;
import br.com.fiap.cuidardeperto.exception.RegraNegocioException;
import br.com.fiap.cuidardeperto.repository.CuidadorRepository;
import br.com.fiap.cuidardeperto.repository.ResponsavelRepository;
import br.com.fiap.cuidardeperto.repository.UsuarioRepository;
import br.com.fiap.cuidardeperto.security.JwtService;
import br.com.fiap.cuidardeperto.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CuidadorRepository cuidadorRepository;
    private final ResponsavelRepository responsavelRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest req) {
        if (req.papel() != Papel.CUIDADOR && req.papel() != Papel.RESPONSAVEL) {
            throw new RegraNegocioException("O cadastro aceita apenas os papeis CUIDADOR ou RESPONSAVEL");
        }
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Ja existe uma conta com o e-mail " + email);
        }

        Usuario usuario = novoUsuario(email, req.senha(), req.papel());

        if (req.papel() == Papel.CUIDADOR) {
            Cuidador c = new Cuidador();
            c.setNome(req.nome().trim());
            c.setEmail(email);
            c.setTelefone(req.telefone());
            c.setRegistroProfissional(req.registroProfissional());
            c.setUsuario(usuario);
            return new UsuarioResponse(usuario.getId(), email, Papel.CUIDADOR, cuidadorRepository.save(c).getId());
        }
        Responsavel r = new Responsavel();
        r.setNome(req.nome().trim());
        r.setEmail(email);
        r.setTelefone(req.telefone());
        r.setUsuario(usuario);
        return new UsuarioResponse(usuario.getId(), email, Papel.RESPONSAVEL, responsavelRepository.save(r).getId());
    }


    @Transactional
    public Usuario novoUsuario(String email, String senha, Papel papel) {
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ConflitoException("Ja existe uma conta com o e-mail " + emailNormalizado);
        }
        Usuario u = new Usuario();
        u.setEmail(emailNormalizado);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setPapel(papel);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }


    public TokenResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.senha()));
        UsuarioPrincipal principal = (UsuarioPrincipal) auth.getPrincipal();
        return new TokenResponse(jwtService.gerar(principal), "Bearer", jwtService.getExpiracaoSegundos(),
                principal.getId(), principal.getUsername(), principal.getPapel());
    }
}
