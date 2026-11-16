package br.com.fiap.cuidardeperto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "idoso_responsavel",
        uniqueConstraints = @UniqueConstraint(name = "uk_idoso_responsavel", columnNames = {"idoso_id", "responsavel_id"}))
@Getter
@Setter
@NoArgsConstructor
public class IdosoResponsavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @Column(nullable = false, length = 40)
    private String parentesco;


    @Column(name = "prioridade_contato", nullable = false)
    private Integer prioridadeContato;
}
