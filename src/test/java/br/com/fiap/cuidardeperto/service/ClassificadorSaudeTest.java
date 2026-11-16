package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Gravidade;
import br.com.fiap.cuidardeperto.domain.Idoso;
import br.com.fiap.cuidardeperto.domain.TipoSinal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confere o classificador contra a tabela da secao 5 da especificacao (limites padrao). */
class ClassificadorSaudeTest {

    private final ClassificadorSaude classificador = new ClassificadorSaude();
    private final Idoso idoso = new Idoso(); // limites padrao

    private Gravidade gravidade(TipoSinal tipo, double valor) {
        return classificador.classificar(idoso, tipo, valor).gravidade();
    }

    @ParameterizedTest(name = "batimento {0} bpm -> {1}")
    @CsvSource(nullValues = "NORMAL", value = {
            "60,NORMAL", "100,NORMAL", "80,NORMAL",
            "59,MEDIA", "50,MEDIA", "101,MEDIA", "120,MEDIA",
            "49,ALTA", "40,ALTA", "121,ALTA", "140,ALTA",
            "39,CRITICA", "141,CRITICA"})
    void batimentos(double valor, Gravidade esperado) {
        assertEquals(esperado, gravidade(TipoSinal.BATIMENTO, valor));
    }

    @ParameterizedTest(name = "glicemia {0} mg/dL -> {1}")
    @CsvSource(nullValues = "NORMAL", value = {
            "70,NORMAL", "140,NORMAL",
            "69,MEDIA", "60,MEDIA", "141,MEDIA", "200,MEDIA",
            "59,ALTA", "54,ALTA", "201,ALTA", "300,ALTA",
            "53,CRITICA", "301,CRITICA"})
    void glicemia(double valor, Gravidade esperado) {
        assertEquals(esperado, gravidade(TipoSinal.GLICEMIA, valor));
    }

    @ParameterizedTest(name = "SpO2 {0}% -> {1}")
    @CsvSource(nullValues = "NORMAL", value = {
            "100,NORMAL", "95,NORMAL",
            "94,MEDIA", "92,MEDIA",
            "91,ALTA", "88,ALTA",
            "87,CRITICA"})
    void spo2(double valor, Gravidade esperado) {
        assertEquals(esperado, gravidade(TipoSinal.SPO2, valor));
    }

    @Test
    void quedaDetectadaEAltaESemRespostaECritica() {
        assertEquals(Gravidade.ALTA, gravidade(TipoSinal.QUEDA, 1));
        assertEquals(Gravidade.CRITICA, gravidade(TipoSinal.QUEDA, 2));
    }

    @Test
    void botaoDePanicoEhSempreCritico() {
        assertEquals(Gravidade.CRITICA, gravidade(TipoSinal.BOTAO_PANICO, 1));
    }

    @Test
    void leituraNormalNaoGeraAlerta() {
        ClassificadorSaude.Resultado r = classificador.classificar(idoso, TipoSinal.BATIMENTO, 75);
        assertTrue(r.normal());
        assertNull(r.gravidade());
    }

    @Test
    void usaOsLimitesDoProprioIdoso() {
        idoso.setFcMax(110); // 105 bpm passa a ser normal para este idoso
        assertNull(gravidade(TipoSinal.BATIMENTO, 105));
        assertEquals(Gravidade.MEDIA, gravidade(TipoSinal.BATIMENTO, 115));
    }
}
