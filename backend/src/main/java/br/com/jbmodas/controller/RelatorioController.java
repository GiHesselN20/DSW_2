package br.com.jbmodas.controller;

import br.com.jbmodas.model.entity.Produto;
import br.com.jbmodas.service.RelatorioService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas de painel e relatórios (somente consulta). */
@RestController
public class RelatorioController {

  private final RelatorioService service;

  public RelatorioController(RelatorioService service) {
    this.service = service;
  }

  /** GET /dashboard -> números gerais e resumo do mês atual. */
  @GetMapping("/dashboard")
  public Map<String, Object> dashboard() {
    return service.dashboard();
  }

  /** GET /relatorios/vendas?inicio=2026-10-01&fim=2026-10-31 -> quantidade e total vendido. */
  @GetMapping("/relatorios/vendas")
  public Map<String, Object> vendas(@RequestParam LocalDate inicio, @RequestParam LocalDate fim) {
    return service.vendas(inicio, fim);
  }

  /** GET /relatorios/financeiro?inicio=...&fim=... -> entradas, saídas e saldo pagos no período. */
  @GetMapping("/relatorios/financeiro")
  public Map<String, Object> financeiro(
      @RequestParam LocalDate inicio, @RequestParam LocalDate fim) {
    return service.financeiro(inicio, fim);
  }

  /** GET /relatorios/estoque?limite=5 -> produtos com estoque igual ou abaixo do limite. */
  @GetMapping("/relatorios/estoque")
  public List<Produto> estoque(@RequestParam(defaultValue = "5") int limite) {
    return service.estoque(limite);
  }
}
