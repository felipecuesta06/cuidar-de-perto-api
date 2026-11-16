package br.com.fiap.cuidardeperto.domain;

public enum TipoAlerta {
    BATIMENTO, GLICEMIA, SPO2, PRESSAO, QUEDA, EMERGENCIA, LEMBRETE_NAO_CONFIRMADO;

    public static TipoAlerta de(TipoSinal sinal) {
        return switch (sinal) {
            case BATIMENTO -> BATIMENTO;
            case GLICEMIA -> GLICEMIA;
            case SPO2 -> SPO2;
            case PRESSAO -> PRESSAO;
            case QUEDA -> QUEDA;
            case BOTAO_PANICO -> EMERGENCIA;
        };
    }
}
