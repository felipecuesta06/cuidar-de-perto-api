package br.com.fiap.cuidardeperto.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI cuidarDePertoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cuidar de Perto API")
                        .version("1.0.0")
                        .description("Monitoramento de rotina e emergencia de idosos que vivem sozinhos, com alertas "
                                + "automaticos para cuidadores e responsaveis. ODS 3 e 10 - FIAP Global Solution 2026/2. "
                                + "Faca login em POST /api/auth/login, clique em Authorize e cole o token."))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
