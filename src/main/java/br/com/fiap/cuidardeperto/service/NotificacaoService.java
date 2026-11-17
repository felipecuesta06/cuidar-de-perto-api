package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Alerta;


public interface NotificacaoService {

    void notificar(Alerta alerta, Destinatario destinatario);

    record Destinatario(String papel, String nome, String email, String telefone) {

        public String descricao() {
            return papel + ": " + nome + " <" + email + ">";
        }
    }
}
