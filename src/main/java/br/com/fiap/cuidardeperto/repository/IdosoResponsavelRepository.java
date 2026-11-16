package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.IdosoResponsavel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IdosoResponsavelRepository extends JpaRepository<IdosoResponsavel, Long> {

    Page<IdosoResponsavel> findByIdosoId(Long idosoId, Pageable pageable);

    Optional<IdosoResponsavel> findByIdosoIdAndResponsavelId(Long idosoId, Long responsavelId);

    boolean existsByIdosoIdAndResponsavelId(Long idosoId, Long responsavelId);


    @Query("SELECT v FROM IdosoResponsavel v JOIN FETCH v.responsavel WHERE v.idoso.id = :idosoId ORDER BY v.prioridadeContato ASC")
    List<IdosoResponsavel> findComResponsavelByIdosoId(@Param("idosoId") Long idosoId);

    @Query("SELECT COUNT(v) > 0 FROM IdosoResponsavel v WHERE v.idoso.id = :idosoId AND v.responsavel.usuario.id = :usuarioId")
    boolean responsavelVinculado(@Param("idosoId") Long idosoId, @Param("usuarioId") Long usuarioId);
}
