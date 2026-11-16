package br.com.fiap.cuidardeperto.service;

import br.com.fiap.cuidardeperto.domain.Alerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoSimuladaService implements NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoSimuladaService.class);

    @Override
    public void notificar(Alerta alerta, Destinatario destinatario) {
        log.info("[NOTIFICACAO SIMULADA] alerta={} gravidade={} tipo={} -> {} (tel: {}) | {}",
                alerta.getId(), alerta.getGravidade(), alerta.getTipo(), destinatario.descricao(),
                destinatario.telefone(), alerta.getMensagem());
    }
}
