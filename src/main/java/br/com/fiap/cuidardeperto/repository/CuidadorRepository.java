package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.Cuidador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuidadorRepository extends JpaRepository<Cuidador, Long> {

    @Query("SELECT c FROM Cuidador c WHERE c.usuario.id = :usuarioId")
    Optional<Cuidador> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) > 0 FROM Cuidador c WHERE c.id = :id AND c.usuario.id = :usuarioId")
    boolean pertenceAoUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
