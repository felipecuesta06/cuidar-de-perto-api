package br.com.fiap.cuidardeperto.exception;


public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
