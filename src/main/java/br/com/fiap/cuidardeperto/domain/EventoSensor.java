package br.com.fiap.cuidardeperto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "evento_sensor",
        indexes = @Index(name = "idx_evento_idoso_data", columnList = "idoso_id, data_hora"))
@Getter
@Setter
@NoArgsConstructor
public class EventoSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSinal tipo;

    @Column(nullable = false)
    private Double valor;

    @Column(length = 20)
    private String unidade;

    @Column(length = 80)
    private String dispositivo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;


    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gravidade gravidade;

    @OneToMany(mappedBy = "evento", fetch = FetchType.LAZY)
    private java.util.List<Alerta> alertas = new java.util.ArrayList<>();
}
