package br.com.fiap.cuidardeperto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "lembrete_remedio")
@Getter
@Setter
@NoArgsConstructor
public class LembreteRemedio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;

    @Column(nullable = false, length = 120)
    private String medicamento;

    @Column(nullable = false, length = 80)
    private String dosagem;

    @Column(nullable = false)
    private LocalTime horario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequencia frequencia = Frequencia.DIARIA;

    @Column(nullable = false)
    private LocalDate inicio;

    private LocalDate fim;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultima_confirmacao")
    private LocalDateTime ultimaConfirmacao;


    @Column(name = "omissoes_seguidas", nullable = false)
    private int omissoesSeguidas = 0;


    @Column(name = "ultima_verificacao")
    private LocalDate ultimaVerificacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();


    public boolean aplicaEm(LocalDate data) {
        if (data.isBefore(inicio) || (fim != null && data.isAfter(fim))) {
            return false;
        }
        long dias = ChronoUnit.DAYS.between(inicio, data);
        return switch (frequencia) {
            case DIARIA -> true;
            case DIAS_ALTERNADOS -> dias % 2 == 0;
            case SEMANAL -> dias % 7 == 0;
        };
    }

    public boolean confirmadoEm(LocalDate data) {
        return ultimaConfirmacao != null && ultimaConfirmacao.toLocalDate().equals(data);
    }
}
