package br.com.fiap.cuidardeperto.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoramentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoramentoScheduler.class);

    private final LembreteService lembreteService;
    private final AlertaService alertaService;

    @Scheduled(fixedDelayString = "${app.monitor.intervalo-ms:60000}",
            initialDelayString = "${app.monitor.intervalo-ms:60000}")
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        int omissoes = lembreteService.verificarOmissoes(agora);
        int escalados = alertaService.escalarPendentes(agora);
        if (omissoes > 0 || escalados > 0) {
            log.info("Monitoramento: {} lembrete(s) nao confirmado(s), {} alerta(s) escalado(s)", omissoes, escalados);
        }
    }
}
