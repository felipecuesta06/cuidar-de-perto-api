package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Alerta;

/**
 * Canal de notificacao. Nesta versao e SIMULADO (log + registro no alerta); para trocar por push, SMS ou e-mail
 * basta criar outra implementacao desta interface, sem alterar o dominio.
 */
public interface NotificacaoService {

    void notificar(Alerta alerta, Destinatario destinatario);

    record Destinatario(String papel, String nome, String email, String telefone) {
        /** Texto gravado no alerta (lista de notificados). */
        public String descricao() {
            return papel + ": " + nome + " <" + email + ">";
        }
    }
}
