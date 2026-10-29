package br.com.jbmodas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de partida da aplicação. Rode esta classe para subir a API em http://localhost:8080.
 * O Spring procura sozinho todas as classes anotadas (controllers, services, repositories...)
 * dentro do pacote br.com.jbmodas.
 */
@SpringBootApplication
public class JbModasApplication {
  public static void main(String[] args) {
    SpringApplication.run(JbModasApplication.class, args);
  }
}
