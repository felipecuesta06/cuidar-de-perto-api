package br.com.fiap.cuidardeperto.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
@EnableCaching
public class CacheConfig {

    public static final String IDOSO_ESTADO = "idoso-estado";
    public static final String PAINEL_CUIDADOR = "painel-cuidador";
    public static final String LEMBRETES_DIA = "lembretes-dia";

    @Bean
    public CacheManager cacheManager(@Value("${app.cache.ttl-seconds:45}") long ttlSegundos) {
        CaffeineCacheManager manager = new CaffeineCacheManager(IDOSO_ESTADO, PAINEL_CUIDADOR, LEMBRETES_DIA);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSegundos))
                .maximumSize(1_000)
                .recordStats());
        return manager;
    }
}
