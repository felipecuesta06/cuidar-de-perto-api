package br.com.fiap.cuidardeperto.controller;

import br.com.fiap.cuidardeperto.dto.SensorDTO.EventoSensorRequest;
import br.com.fiap.cuidardeperto.dto.SensorDTO.EventoSensorResponse;
import br.com.fiap.cuidardeperto.service.EventoSensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/eventos-sensor")
@RequiredArgsConstructor
@Tag(name = "Sensores", description = "Recebimento de leituras dos dispositivos")
public class EventoSensorController {

    private final EventoSensorService eventoSensorService;

    @Operation(summary = "Recebe leitura de sensor, classifica a gravidade e gera alerta se preciso",
            description = "O dispositivo envia o token do idoso ao qual esta associado. "
                    + "Cuidador vinculado/admin tambem podem enviar (simulacao).",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Leitura registrada (alertaId preenchido se gerou alerta)"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "403", description = "Sem permissao sobre o idoso"),
                    @ApiResponse(responseCode = "404", description = "Idoso nao encontrado"),
                    @ApiResponse(responseCode = "422", description = "Valor obrigatorio para o sinal")})
    @PostMapping
    @PreAuthorize("@acesso.podeRegistrar(#req.idosoId())")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoSensorResponse registrar(@Valid @RequestBody EventoSensorRequest req) {
        return eventoSensorService.registrar(req);
    }
}
