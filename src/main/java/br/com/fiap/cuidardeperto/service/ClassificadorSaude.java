package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.TipoAlerta;
import br.com.fiap.cuidardeperto.domain.TipoSinal;
import org.springframework.stereotype.Component;


@Component
public class ClassificadorSaude {


    private static final double[] FC_ABAIXO = {10, 20};
    private static final double[] FC_ACIMA = {20, 40};
    private static final double[] GLICEMIA_ABAIXO = {10, 16};
    private static final double[] GLICEMIA_ACIMA = {60, 160};
    private static final double[] SPO2_ABAIXO = {3, 7};
    private static final double[] PRESSAO_ACIMA = {20, 40};

    private static final double PRESSAO_SIST_MINIMA = 90;
    private static final double[] PRESSAO_ABAIXO = {10, 20};


    public record Resultado(Gravidade gravidade, TipoAlerta tipoAlerta, String mensagem) {
        public boolean normal() {
            return gravidade == null;
        }
    }

    public Resultado classificar(Idoso idoso, TipoSinal tipo, double valor) {
        return switch (tipo) {
            case BATIMENTO -> faixa(tipo, "Batimentos cardiacos", valor, "bpm",
                    idoso.getFcMin(), idoso.getFcMax(), FC_ABAIXO, FC_ACIMA);
            case GLICEMIA -> faixa(tipo, "Glicemia", valor, "mg/dL",
                    idoso.getGlicemiaMin(), idoso.getGlicemiaMax(), GLICEMIA_ABAIXO, GLICEMIA_ACIMA);
            case SPO2 -> faixa(tipo, "Saturacao de oxigenio (SpO2)", valor, "%",
                    idoso.getSpo2Min(), null, SPO2_ABAIXO, null);
            case PRESSAO -> faixa(tipo, "Pressao sistolica", valor, "mmHg",
                    (int) PRESSAO_SIST_MINIMA, idoso.getPressaoSistMax(), PRESSAO_ABAIXO, PRESSAO_ACIMA);
            case QUEDA -> valor >= 2
                    ? new Resultado(Gravidade.CRITICA, TipoAlerta.QUEDA, "Queda sem resposta do idoso em 2 minutos")
                    : new Resultado(Gravidade.ALTA, TipoAlerta.QUEDA, "Queda detectada");
            case BOTAO_PANICO -> new Resultado(Gravidade.CRITICA, TipoAlerta.EMERGENCIA,
                    "Botao de panico acionado pelo idoso");
        };
    }

    private Resultado faixa(TipoSinal tipo, String rotulo, double valor, String unidade,
                            Integer min, Integer max, double[] abaixo, double[] acima) {
        TipoAlerta tipoAlerta = TipoAlerta.de(tipo);
        if (min != null && valor < min) {
            Gravidade g = porDesvio(min - valor, abaixo);
            return new Resultado(g, tipoAlerta,
                    rotulo + " de " + fmt(valor) + " " + unidade + " abaixo do limite (" + min + " " + unidade + ")");
        }
        if (max != null && acima != null && valor > max) {
            Gravidade g = porDesvio(valor - max, acima);
            return new Resultado(g, tipoAlerta,
                    rotulo + " de " + fmt(valor) + " " + unidade + " acima do limite (" + max + " " + unidade + ")");
        }
        return new Resultado(null, tipoAlerta, rotulo + " normal");
    }

    private Gravidade porDesvio(double desvio, double[] margens) {
        if (desvio <= margens[0]) {
            return Gravidade.MEDIA;
        }
        if (desvio <= margens[1]) {
            return Gravidade.ALTA;
        }
        return Gravidade.CRITICA;
    }

    private String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
