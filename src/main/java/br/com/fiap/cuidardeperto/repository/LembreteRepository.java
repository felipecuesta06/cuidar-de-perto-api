package br.com.fiap.cuidardeperto.repository;

import br.com.fiap.cuidardeperto.domain.LembreteRemedio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LembreteRepository extends JpaRepository<LembreteRemedio, Long> {


    List<LembreteRemedio> findByIdosoIdAndAtivoTrue(Long idosoId);

    Page<LembreteRemedio> findByIdosoId(Long idosoId, Pageable pageable);


    @Query("SELECT l FROM LembreteRemedio l JOIN FETCH l.idoso i WHERE l.ativo = true AND i.ativo = true")
    List<LembreteRemedio> findAtivosComIdoso();
}
