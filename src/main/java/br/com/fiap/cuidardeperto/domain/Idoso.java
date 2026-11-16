package br.com.fiap.cuidardeperto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "idoso")
@Getter
@Setter
@NoArgsConstructor
public class Idoso {

    public static final int FC_MIN_PADRAO = 60;
    public static final int FC_MAX_PADRAO = 100;
    public static final int GLICEMIA_MIN_PADRAO = 70;
    public static final int GLICEMIA_MAX_PADRAO = 140;
    public static final int SPO2_MIN_PADRAO = 95;
    public static final int PRESSAO_SIST_MAX_PADRAO = 140;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(length = 11)
    private String telefone;

    @Column(length = 250)
    private String endereco;

    @Column(name = "condicoes_medicas", length = 1000)
    private String condicoesMedicas;

    @Column(nullable = false)
    private boolean ativo = true;

    // ---- limites de saude (ajustaveis por idoso) ----
    @Column(name = "fc_min", nullable = false)
    private Integer fcMin = FC_MIN_PADRAO;

    @Column(name = "fc_max", nullable = false)
    private Integer fcMax = FC_MAX_PADRAO;

    @Column(name = "glicemia_min", nullable = false)
    private Integer glicemiaMin = GLICEMIA_MIN_PADRAO;

    @Column(name = "glicemia_max", nullable = false)
    private Integer glicemiaMax = GLICEMIA_MAX_PADRAO;

    @Column(name = "spo2_min", nullable = false)
    private Integer spo2Min = SPO2_MIN_PADRAO;

    @Column(name = "pressao_sist_max", nullable = false)
    private Integer pressaoSistMax = PRESSAO_SIST_MAX_PADRAO;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "idoso_cuidador",
            joinColumns = @JoinColumn(name = "idoso_id"),
            inverseJoinColumns = @JoinColumn(name = "cuidador_id"))
    private Set<Cuidador> cuidadores = new HashSet<>();


    @OneToMany(mappedBy = "idoso", fetch = FetchType.LAZY)
    private Set<IdosoResponsavel> responsaveis = new HashSet<>();

    @OneToMany(mappedBy = "idoso", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LembreteRemedio> lembretes = new ArrayList<>();

    @OneToMany(mappedBy = "idoso", fetch = FetchType.LAZY)
    private List<EventoSensor> eventos = new ArrayList<>();

    @OneToMany(mappedBy = "idoso", fetch = FetchType.LAZY)
    private List<Alerta> alertas = new ArrayList<>();

    public int idade() {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
