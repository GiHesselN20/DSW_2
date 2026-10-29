package br.com.jbmodas.controller;

import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.service.FinanceiroService;
import br.com.jbmodas.service.UsuarioService;
import br.com.jbmodas.util.Paginas;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Rotas HTTP do financeiro (/financeiro): entradas e saídas de dinheiro. */
@RestController
@RequestMapping("/financeiro")
public class FinanceiroController {

  private final FinanceiroService service;
  private final UsuarioService usuarios;

  public FinanceiroController(FinanceiroService service, UsuarioService usuarios) {
    this.service = service;
    this.usuarios = usuarios;
  }

  /** GET /financeiro?pagina=0&tamanho=20 -> lista paginada (inclui as entradas das vendas). */
  @GetMapping
  public Page<FinanceiroService.Resumo> listar(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return service.listar(Paginas.de(pagina, tamanho));
  }

  /** GET /financeiro/1 -> um lançamento. */
  @GetMapping("/{id}")
  public FinanceiroService.Resumo ver(@PathVariable Long id) {
    return service.ver(id);
  }

  /** POST /financeiro -> novo lançamento manual. O usuário responsável é o do token. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FinanceiroService.Resumo criar(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dados.Financeiro d) {
    return service.salvar(null, d, usuarios.atual(jwt));
  }

  /** PUT /financeiro/1 -> atualiza (ex.: marcar como PAGO e informar a data de pagamento). */
  @PutMapping("/{id}")
  public FinanceiroService.Resumo atualizar(
      @PathVariable Long id,
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody Dados.Financeiro d) {
    return service.salvar(id, d, usuarios.atual(jwt));
  }

  /** DELETE /financeiro/1 -> exclui um lançamento manual (os de venda são protegidos). */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable Long id) {
    service.excluir(id);
  }
}
