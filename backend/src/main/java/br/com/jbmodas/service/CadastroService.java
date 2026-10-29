package br.com.jbmodas.service;

import br.com.jbmodas.model.entity.Cliente;
import br.com.jbmodas.model.entity.Produto;
import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.repository.ClienteRepository;
import br.com.jbmodas.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cadastros simples: clientes e produtos (criar, consultar, alterar, excluir).
 * Fluxo de uma chamada: Controller -> este Service -> Repository -> MySQL.
 * Os dados chegam já validados (Dados.java) e viram entidades aqui.
 */
@Service
public class CadastroService {

  private final ClienteRepository clienteRepo;
  private final ProdutoRepository produtoRepo;

  public CadastroService(ClienteRepository clienteRepo, ProdutoRepository produtoRepo) {
    this.clienteRepo = clienteRepo;
    this.produtoRepo = produtoRepo;
  }

  /** Cria o erro 404 "X não encontrado". Usado também por outros services. */
  public static ResponseStatusException ausente(String nome) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, nome + " não encontrado");
  }

  /** Busca um cliente pelo id; se não existir, responde 404. */
  public Cliente cliente(Long id) {
    return clienteRepo.findById(id).orElseThrow(() -> ausente("Cliente"));
  }

  /** Busca um produto pelo id; se não existir, responde 404. */
  public Produto produto(Long id) {
    return produtoRepo.findById(id).orElseThrow(() -> ausente("Produto"));
  }

  /** Lista clientes por página (Pageable diz qual página e quantos registros). */
  public Page<Cliente> clientes(Pageable pagina) {
    return clienteRepo.findAll(pagina);
  }

  /** Lista produtos por página. */
  public Page<Produto> produtos(Pageable pagina) {
    return produtoRepo.findAll(pagina);
  }

  /**
   * Cria OU atualiza um cliente. id == null -> cliente novo (INSERT);
   * id preenchido -> carrega o existente e troca os campos (UPDATE).
   */
  @Transactional
  public Cliente salvarCliente(Long id, Dados.Cliente d) {
    Cliente c = id == null ? new Cliente() : cliente(id);
    c.nome = d.nome();
    c.telefone = d.telefone();
    c.endereco = d.endereco();
    return clienteRepo.save(c);
  }

  /**
   * Cria OU atualiza um produto. Ao atualizar, a linha é travada no banco
   * (buscarComBloqueio) para não conflitar com uma venda acontecendo ao mesmo tempo.
   * Também serve para corrigir a quantidade em estoque.
   */
  @Transactional
  public Produto salvarProduto(Long id, Dados.Produto d) {
    Produto p =
        id == null
            ? new Produto()
            : produtoRepo.buscarComBloqueio(id).orElseThrow(() -> ausente("Produto"));
    p.nome = d.nome();
    p.categoria = d.categoria();
    p.tamanho = d.tamanho();
    p.cor = d.cor();
    p.preco = d.preco();
    p.quantidadeEstoque = d.quantidadeEstoque();
    p.disponivel = d.disponivel();
    return produtoRepo.save(p);
  }

  /**
   * Exclui o cliente. flush() força o DELETE agora; se o cliente já tem pedidos ou vendas,
   * o banco recusa e a API responde 409 (veja util/Erros.java).
   */
  @Transactional
  public void excluirCliente(Long id) {
    clienteRepo.delete(cliente(id));
    clienteRepo.flush();
  }

  /** Exclui o produto. Se ele já foi vendido, o banco recusa (409): marque disponivel=false. */
  @Transactional
  public void excluirProduto(Long id) {
    produtoRepo.delete(produto(id));
    produtoRepo.flush();
  }
}
