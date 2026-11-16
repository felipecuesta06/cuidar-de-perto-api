package br.com.fiap.cuidardeperto.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Entity
@Table(name = "alerta",
        indexes = @Index(name = "idx_alerta_idoso_status", columnList = "idoso_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    private static final String SEPARADOR = "; ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idoso_id", nullable = false)
    private Idoso idoso;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoSensor evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gravidade gravidade;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAlerta status = StatusAlerta.ABERTO;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;


    @Column(length = 1000)
    private String notificados;

    @Column(length = 500)
    private String observacao;


    @Column(nullable = false)
    private int ocorrencias = 1;


    @Column(nullable = false)
    private boolean escalonado = false;

    public List<String> notificadosLista() {
        if (notificados == null || notificados.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(notificados.split(SEPARADOR)));
    }

    public boolean jaNotificado(String destinatario) {
        return notificadosLista().contains(destinatario);
    }

    public void registrarNotificado(String destinatario) {
        List<String> lista = notificadosLista();
        if (!lista.contains(destinatario)) {
            lista.add(destinatario);
            this.notificados = String.join(SEPARADOR, lista);
        }
    }
}
