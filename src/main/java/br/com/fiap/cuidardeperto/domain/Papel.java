package br.com.fiap.cuidardeperto.domain;

public enum Papel {
    ADMIN, CUIDADOR, RESPONSAVEL, IDOSO;

    public String authority() {
        return "ROLE_" + name();
    }
}
