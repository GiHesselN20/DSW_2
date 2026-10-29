package br.com.jbmodas.service;

import br.com.jbmodas.model.entity.Financeiro;
import br.com.jbmodas.model.entity.Produto;
import br.com.jbmodas.repository.ClienteRepository;
import br.com.jbmodas.repository.FinanceiroRepository;
import br.com.jbmodas.repository.PedidoRepository;
import br.com.jbmodas.repository.ProdutoRepository;
import br.com.jbmodas.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Relatórios e painel (dashboard). Só LEEM dados já existentes e fazem as contas em memória;
 * não existe tabela de relatório. readOnly = nada aqui altera o banco.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

  private final VendaRepository vendas;
  private final ProdutoRepository produtos;
  private final FinanceiroRepository financeiro;
  private final ClienteRepository clientes;
  private final PedidoRepository pedidos;

  public RelatorioService(
      VendaRepository vendas,
      ProdutoRepository produtos,
      FinanceiroRepository financeiro,
      ClienteRepository clientes,
      PedidoRepository pedidos) {
    this.vendas = vendas;
    this.produtos = produtos;
    this.financeiro = financeiro;
    this.clientes = clientes;
    this.pedidos = pedidos;
  }

  // A data inicial não pode ser depois da final.
  private void datas(LocalDate inicio, LocalDate fim) {
    if (inicio.isAfter(fim))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data inicial maior que final");
  }

  /** Quantidade de vendas e total vendido no período. Vendas CANCELADAS não entram. */
  public Map<String, Object> vendas(LocalDate inicio, LocalDate fim) {
    datas(inicio, fim);
    var lista =
        vendas.findAll().stream()
            .filter(
                v ->
                    v.status.equals("CONCLUIDA")
                        && !v.data.toLocalDate().isBefore(inicio)
                        && !v.data.toLocalDate().isAfter(fim))
            .toList();
    BigDecimal total = lista.stream().map(v -> v.total).reduce(BigDecimal.ZERO, BigDecimal::add);
    return Map.of(
        "inicio", inicio, "fim", fim, "quantidadeVendas", lista.size(), "totalVendido", total);
  }

  /** Produtos com estoque igual ou abaixo do limite (padrão 5): serve de alerta de reposição. */
  public List<Produto> estoque(int limite) {
    if (limite < 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite deve ser positivo ou zero");
    return produtos.findAll().stream().filter(p -> p.quantidadeEstoque <= limite).toList();
  }

  /**
   * Entradas, saídas e saldo do período. Conta só lançamentos PAGOS, pela data de pagamento.
   * Atenção: saldo não é lucro, pois o sistema não guarda o custo das peças.
   */
  public Map<String, Object> financeiro(LocalDate inicio, LocalDate fim) {
    datas(inicio, fim);
    BigDecimal entradas = BigDecimal.ZERO;
    BigDecimal saidas = BigDecimal.ZERO;
    for (Financeiro f : financeiro.findAll()) {
      if (!f.status.equals("PAGO")
          || f.dataPagamento.isBefore(inicio)
          || f.dataPagamento.isAfter(fim)) continue; // ignora o que não conta no período
      if (f.tipo.equals("ENTRADA")) entradas = entradas.add(f.valor);
      else saidas = saidas.add(f.valor);
    }
    return Map.of(
        "inicio", inicio,
        "fim", fim,
        "entradas", entradas,
        "saidas", saidas,
        "saldo", entradas.subtract(saidas));
  }

  /** Painel inicial: números gerais e o resumo do mês atual (do dia 1º até hoje). */
  public Map<String, Object> dashboard() {
    LocalDate hoje = LocalDate.now();
    return Map.of(
        "clientes", clientes.count(),
        "produtos", produtos.count(),
        "pedidosAbertos", pedidos.findAll().stream().filter(p -> p.status.equals("ABERTO")).count(),
        "vendasDoMes", vendas(hoje.withDayOfMonth(1), hoje),
        "financeiroDoMes", financeiro(hoje.withDayOfMonth(1), hoje),
        "produtosComEstoqueBaixo", estoque(5).size());
  }
}
