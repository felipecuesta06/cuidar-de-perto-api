package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.Responsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {

    @Query("SELECT r FROM Responsavel r WHERE r.usuario.id = :usuarioId")
    Optional<Responsavel> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(r) > 0 FROM Responsavel r WHERE r.id = :id AND r.usuario.id = :usuarioId")
    boolean pertenceAoUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
