package br.com.jbmodas.controller;

import br.com.jbmodas.model.entity.Cliente;
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

/**
 * Rotas HTTP dos clientes. O controller só recebe a requisição e repassa ao service.
 * @RestController = responde em JSON.  @RequestMapping("/clientes") = prefixo de todas as rotas.
 * Todas exigem token (regra em util/Seguranca.java).
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

  private final CadastroService service;

  public ClienteController(CadastroService service) {
    this.service = service;
  }

  /** GET /clientes?pagina=0&tamanho=20 -> lista paginada (mais novos primeiro). */
  @GetMapping
  public Page<Cliente> listar(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return service.clientes(Paginas.de(pagina, tamanho));
  }

  /** GET /clientes/1 -> um cliente. O {id} da URL chega no parâmetro (@PathVariable). */
  @GetMapping("/{id}")
  public Cliente buscar(@PathVariable Long id) {
    return service.cliente(id);
  }

  /** POST /clientes -> o JSON do corpo (@RequestBody) é validado e cadastrado. Responde 201. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Cliente criar(@Valid @RequestBody Dados.Cliente d) {
    return service.salvarCliente(null, d);
  }

  /** PUT /clientes/1 -> atualiza o cliente 1 com o JSON enviado. */
  @PutMapping("/{id}")
  public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody Dados.Cliente d) {
    return service.salvarCliente(id, d);
  }

  /** DELETE /clientes/1 -> exclui. Responde 204 (sem corpo), ou 409 se o cliente tiver histórico. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void excluir(@PathVariable Long id) {
    service.excluirCliente(id);
  }
}
