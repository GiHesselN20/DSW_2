package br.com.jbmodas.controller;

import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.service.ComercialService;
import br.com.jbmodas.service.UsuarioService;
import br.com.jbmodas.util.Paginas;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de PEDIDOS e VENDAS. Nas que criam algo, o usuário responsável vem do token
 * (@AuthenticationPrincipal Jwt) e nunca do JSON enviado.
 */
@RestController
public class ComercialController {

  private final ComercialService service;
  private final UsuarioService usuarios;

  public ComercialController(ComercialService service, UsuarioService usuarios) {
    this.service = service;
    this.usuarios = usuarios;
  }

  // ----------------------------- PEDIDOS -----------------------------

  /** GET /pedidos?pagina=0&tamanho=20 -> lista paginada, cada pedido com seus itens. */
  @GetMapping("/pedidos")
  public Page<ComercialService.PedidoResumo> pedidos(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return service.listarPedidos(Paginas.de(pagina, tamanho));
  }

  /** GET /pedidos/1 -> um pedido com itens e total. */
  @GetMapping("/pedidos/{id}")
  public ComercialService.PedidoResumo pedido(@PathVariable Long id) {
    return service.verPedido(id);
  }

  /**
   * POST /pedidos -> cria um pedido ABERTO (não mexe no estoque). Exemplo de corpo:
   * {"clienteId":1,"tipoEntrega":"RETIRADA","frete":0,"itens":[{"produtoId":1,"quantidade":2}]}
   */
  @PostMapping("/pedidos")
  @ResponseStatus(HttpStatus.CREATED)
  public ComercialService.PedidoResumo criarPedido(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dados.Pedido d) {
    return service.criarPedido(d, usuarios.atual(jwt));
  }

  /** POST /pedidos/1/cancelar -> cancela o pedido (só se estiver ABERTO). Sem corpo. */
  @PostMapping("/pedidos/{id}/cancelar")
  public ComercialService.PedidoResumo cancelarPedido(@PathVariable Long id) {
    return service.cancelarPedido(id);
  }

  /**
   * POST /pedidos/1/confirmar -> transforma o pedido em VENDA (baixa estoque, gera financeiro).
   * Corpo: {"formaPagamento":"PIX","canal":"WHATSAPP"}. Responde 201 com a venda criada.
   */
  @PostMapping("/pedidos/{id}/confirmar")
  @ResponseStatus(HttpStatus.CREATED)
  public ComercialService.VendaResumo confirmar(
      @PathVariable Long id,
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody Dados.Confirmar d) {
    return service.confirmarPedido(id, d, usuarios.atual(jwt));
  }

  // ----------------------------- VENDAS -----------------------------

  /** GET /vendas?pagina=0&tamanho=20 -> lista paginada, cada venda com seus itens. */
  @GetMapping("/vendas")
  public Page<ComercialService.VendaResumo> vendas(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return service.listarVendas(Paginas.de(pagina, tamanho));
  }

  /** GET /vendas/1 -> uma venda. */
  @GetMapping("/vendas/{id}")
  public ComercialService.VendaResumo venda(@PathVariable Long id) {
    return service.verVenda(id);
  }

  /**
   * POST /vendas -> venda direta (sem pedido). Baixa o estoque e gera a entrada no financeiro.
   * Corpo: {"clienteId":1,"formaPagamento":"PIX","canal":"LOJA","itens":[{"produtoId":1,"quantidade":2}]}
   */
  @PostMapping("/vendas")
  @ResponseStatus(HttpStatus.CREATED)
  public ComercialService.VendaResumo criarVenda(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dados.Venda d) {
    return service.criarVenda(d, usuarios.atual(jwt));
  }

  /** POST /vendas/1/cancelar -> cancela a venda: devolve o estoque e estorna o financeiro. */
  @PostMapping("/vendas/{id}/cancelar")
  public ComercialService.VendaResumo cancelarVenda(@PathVariable Long id) {
    return service.cancelarVenda(id);
  }
}
