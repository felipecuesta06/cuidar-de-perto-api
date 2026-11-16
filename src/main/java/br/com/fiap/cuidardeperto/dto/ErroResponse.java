package br.com.fiap.cuidardeperto.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;


public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> campos) {

    public record CampoErro(String campo, String mensagem) {
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho) {
        return de(status, mensagem, caminho, List.of());
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho, List<CampoErro> campos) {
        return new ErroResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem, caminho, campos);
    }
}
