package br.com.fiap.cuidardeperto.exception;


public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " nao encontrado(a): " + id);
    }
}
