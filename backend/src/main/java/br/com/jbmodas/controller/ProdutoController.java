package br.com.jbmodas.controller;

import br.com.jbmodas.model.entity.Produto;
import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.service.CadastroService;
import br.com.jbmodas.util.Paginas;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

/** Rotas HTTP dos produtos e do estoque (/produtos). Mesmo padrão do ClienteController. */
@RestController
@RequestMapping("/produtos")
public class ProdutoController {

  private final CadastroService service;

  public ProdutoController(CadastroService service) {
    this.service = service;
  }

  /** GET /produtos?pagina=0&tamanho=20 -> lista paginada. */
  @GetMapping
  public Page<Produto> listar(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return service.produtos(Paginas.de(pagina, tamanho));
  }

  /** GET /produtos/1 -> um produto. */
  @GetMapping("/{id}")
  public Produto buscar(@PathVariable Long id) {
    return service.produto(id);
  }

  /** POST /produtos -> cadastra uma peça (com preço e quantidade em estoque). */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Produto criar(@Valid @RequestBody Dados.Produto d) {
    return service.salvarProduto(null, d);
  }

  /** PUT /produtos/1 -> atualiza dados, preço ou corrige o estoque. */
  @PutMapping("/{id}")
  public Produto atualizar(@PathVariable Long id, @Valid @RequestBody Dados.Produto d) {
    return service.salvarProduto(id, d);
  }

  /** DELETE /produtos/1 -> exclui; se já foi vendido responde 409 (use disponivel=false). */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable Long id) {
    service.excluirProduto(id);
  }
}
