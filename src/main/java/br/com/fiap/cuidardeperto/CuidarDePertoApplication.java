package br.com.fiap.cuidardeperto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CuidarDePertoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuidarDePertoApplication.class, args);
    }
}
