package br.com.jbmodas.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Monta a "página" das listagens (?pagina=0&tamanho=20) de forma segura.
 * Os registros mais novos vêm primeiro (ordem decrescente pelo id).
 */
public class Paginas {

  public static PageRequest de(int pagina, int tamanho) {
    int paginaSegura = Math.max(0, pagina); // nunca negativa
    int tamanhoSeguro = Math.max(1, Math.min(tamanho, 100)); // entre 1 e 100 registros
    return PageRequest.of(paginaSegura, tamanhoSeguro, Sort.by("id").descending());
  }
}
