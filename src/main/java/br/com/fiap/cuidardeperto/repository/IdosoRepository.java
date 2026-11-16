package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.Idoso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdosoRepository extends JpaRepository<Idoso, Long>, JpaSpecificationExecutor<Idoso> {


    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    @Query("SELECT COUNT(i) > 0 FROM Idoso i JOIN i.cuidadores c WHERE i.id = :idosoId AND c.usuario.id = :usuarioId")
    boolean cuidadorVinculado(@Param("idosoId") Long idosoId, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(i) > 0 FROM Idoso i WHERE i.id = :idosoId AND i.usuario.id = :usuarioId")
    boolean ehOProprioIdoso(@Param("idosoId") Long idosoId, @Param("usuarioId") Long usuarioId);
}
