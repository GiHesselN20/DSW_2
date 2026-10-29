package br.com.jbmodas.service;

import br.com.jbmodas.model.entity.Cliente;
import br.com.jbmodas.model.entity.Financeiro;
import br.com.jbmodas.model.entity.ItemPedido;
import br.com.jbmodas.model.entity.ItemVenda;
import br.com.jbmodas.model.entity.Pedido;
import br.com.jbmodas.model.entity.Produto;
import br.com.jbmodas.model.entity.Usuario;
import br.com.jbmodas.model.entity.Venda;
import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.repository.FinanceiroRepository;
import br.com.jbmodas.repository.ItemPedidoRepository;
import br.com.jbmodas.repository.ItemVendaRepository;
import br.com.jbmodas.repository.PedidoRepository;
import br.com.jbmodas.repository.ProdutoRepository;
import br.com.jbmodas.repository.VendaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * O coração do sistema: PEDIDOS e VENDAS.
 *
 * Fluxos principais:
 *  - Pedido:  criarPedido (ABERTO) -> confirmarPedido (vira Venda) ou cancelarPedido.
 *  - Venda:   criarVenda direta (balcão), ou a que nasce da confirmação de um pedido.
 *  - Toda venda: baixa o estoque + cria uma ENTRADA no financeiro.
 *  - cancelarVenda: devolve o estoque e cancela o lançamento financeiro.
 *
 * @Transactional na classe = cada método público é uma TRANSAÇÃO: se qualquer passo falhar
 * (ex.: estoque insuficiente no 2º item), o banco desfaz TUDO que o método já tinha feito.
 */
@Service
@Transactional
public class ComercialService {

  private final PedidoRepository pedidos;
  private final VendaRepository vendas;
  private final ItemPedidoRepository itensPedido;
  private final ItemVendaRepository itensVenda;
  private final ProdutoRepository produtos;
  private final FinanceiroRepository financeiro;
  private final CadastroService cadastros;

  // EntityManager = a "caneta" do JPA; usado abaixo para recarregar o produto do banco.
  @PersistenceContext private EntityManager entityManager;

  public ComercialService(
      PedidoRepository pedidos,
      VendaRepository vendas,
      ItemPedidoRepository itensPedido,
      ItemVendaRepository itensVenda,
      ProdutoRepository produtos,
      FinanceiroRepository financeiro,
      CadastroService cadastros) {
    this.pedidos = pedidos;
    this.vendas = vendas;
    this.itensPedido = itensPedido;
    this.itensVenda = itensVenda;
    this.produtos = produtos;
    this.financeiro = financeiro;
    this.cadastros = cadastros;
  }

  // Erro 409 (conflito): a ação não é permitida no estado atual dos dados.
  private ResponseStatusException conflito(String mensagem) {
    return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
  }

  // ======================================================================
  // FORMATO DAS RESPOSTAS (o que a API devolve). Usam ids em vez de objetos inteiros.
  // ======================================================================

  /** Uma linha de pedido/venda na resposta: qual produto, quantas unidades e o preço. */
  public record Linha(
      Long id, Long produtoId, String nome, Integer quantidade, BigDecimal precoUnitario) {}

  /** Pedido completo para a resposta: dados + itens + total (itens + frete). */
  public record PedidoResumo(
      Long id,
      Long clienteId,
      Long usuarioId,
      LocalDateTime data,
      String status,
      String tipoEntrega,
      String enderecoEntrega,
      BigDecimal frete,
      String observacoes,
      List<Linha> itens,
      BigDecimal total) {}

  /** Venda completa para a resposta. pedidoId é null quando foi venda direta. */
  public record VendaResumo(
      Long id,
      Long clienteId,
      Long usuarioId,
      Long pedidoId,
      LocalDateTime data,
      String status,
      BigDecimal total,
      String formaPagamento,
      String canal,
      List<Linha> itens) {}

  // Soma preço x quantidade de todas as linhas.
  private BigDecimal total(List<Linha> itens) {
    return itens.stream()
        .map(x -> x.precoUnitario().multiply(BigDecimal.valueOf(x.quantidade())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // ======================================================================
  // CONSULTAS
  // ======================================================================

  /** Monta a resposta de um pedido: busca o pedido e seus itens e calcula o total com frete. */
  public PedidoResumo verPedido(Long id) {
    Pedido p = pedidos.findById(id).orElseThrow(() -> CadastroService.ausente("Pedido"));
    List<Linha> itens =
        itensPedido.findByPedidoId(id).stream()
            .map(x -> new Linha(x.id, x.produto.id, x.produto.nome, x.quantidade, x.precoUnitario))
            .toList();
    return new PedidoResumo(
        p.id,
        p.cliente.id,
        p.usuario.id,
        p.data,
        p.status,
        p.tipoEntrega,
        p.enderecoEntrega,
        p.frete,
        p.observacoes,
        itens,
        total(itens).add(p.frete));
  }

  /** Monta a resposta de uma venda com seus itens. */
  public VendaResumo verVenda(Long id) {
    Venda v = vendas.findById(id).orElseThrow(() -> CadastroService.ausente("Venda"));
    List<Linha> itens =
        itensVenda.findByVendaId(id).stream()
            .map(x -> new Linha(x.id, x.produto.id, x.produto.nome, x.quantidade, x.precoUnitario))
            .toList();
    return new VendaResumo(
        v.id,
        v.cliente.id,
        v.usuario.id,
        v.pedido == null ? null : v.pedido.id,
        v.data,
        v.status,
        v.total,
        v.formaPagamento,
        v.canal,
        itens);
  }

  public Page<PedidoResumo> listarPedidos(Pageable pagina) {
    return pedidos.findAll(pagina).map(p -> verPedido(p.id));
  }

  public Page<VendaResumo> listarVendas(Pageable pagina) {
    return vendas.findAll(pagina).map(v -> verVenda(v.id));
  }

  // ======================================================================
  // AUXILIARES DE ESTOQUE
  // ======================================================================

  /**
   * Junta itens repetidos do JSON (mesmo produto duas vezes = soma as quantidades).
   * Usa um mapa ORDENADO pelo id do produto: assim os produtos são sempre travados na mesma ordem,
   * o que evita "deadlock" (duas vendas se esperando para sempre) quando acontecem juntas.
   */
  private SortedMap<Long, Integer> agrupar(List<Dados.Item> itens) {
    SortedMap<Long, Integer> result = new TreeMap<>();
    for (Dados.Item i : itens) {
      try {
        result.merge(i.produtoId(), i.quantidade(), Math::addExact);
      } catch (ArithmeticException e) {
        throw conflito("Quantidade excessiva");
      }
    }
    return result;
  }

  /**
   * Busca o produto TRAVANDO a linha no banco até o fim da transação (ninguém mais altera
   * o estoque dele enquanto esta operação não terminar).
   * O refresh logo depois recarrega os valores atuais do banco: o produto pode já ter sido lido
   * antes de a trava ser obtida, e assim garantimos que NÃO usamos um saldo de estoque antigo.
   */
  private Produto bloquear(Long id) {
    Produto produto =
        produtos.buscarComBloqueio(id).orElseThrow(() -> CadastroService.ausente("Produto"));
    entityManager.refresh(produto, LockModeType.PESSIMISTIC_WRITE);
    return produto;
  }

  // ======================================================================
  // PEDIDOS
  // ======================================================================

  /**
   * Cria um pedido ABERTO. NÃO mexe no estoque. Guarda o preço atual de cada peça no item.
   * usuarioLogado = quem fez a chamada (vem do token), nunca do JSON.
   */
  public PedidoResumo criarPedido(Dados.Pedido d, Usuario usuarioLogado) {
    if (d.tipoEntrega().equals("ENTREGA")
        && (d.enderecoEntrega() == null || d.enderecoEntrega().isBlank()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o endereço de entrega");
    if (d.tipoEntrega().equals("RETIRADA") && d.frete().signum() != 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Retirada não tem frete");

    Pedido p = new Pedido();
    p.cliente = cadastros.cliente(d.clienteId());
    p.usuario = usuarioLogado;
    p.data = LocalDateTime.now();
    p.status = "ABERTO";
    p.tipoEntrega = d.tipoEntrega();
    p.enderecoEntrega = d.enderecoEntrega();
    p.frete = d.frete();
    p.observacoes = d.observacoes();
    pedidos.save(p);

    // Um ItemPedido para cada produto pedido (quantidades repetidas já somadas).
    for (var e : agrupar(d.itens()).entrySet()) {
      Produto produto = cadastros.produto(e.getKey());
      if (!produto.disponivel) throw conflito("Produto indisponível: " + produto.nome);
      ItemPedido i = new ItemPedido();
      i.pedido = p;
      i.produto = produto;
      i.quantidade = e.getValue();
      i.precoUnitario = produto.preco; // congela o preço de hoje
      itensPedido.save(i);
    }
    return verPedido(p.id);
  }

  /** Cancela um pedido. Só é possível enquanto ele estiver ABERTO. */
  public PedidoResumo cancelarPedido(Long id) {
    Pedido p =
        pedidos.buscarComBloqueio(id).orElseThrow(() -> CadastroService.ausente("Pedido"));
    if (!p.status.equals("ABERTO")) throw conflito("Somente pedidos abertos podem ser cancelados");
    p.status = "CANCELADO"; // sem chamar save: o JPA percebe a alteração e faz o UPDATE sozinho
    return verPedido(id);
  }

  /**
   * CONFIRMA um pedido: ele vira uma Venda. Mantém os preços gravados no pedido,
   * baixa o estoque, soma o frete ao total e cria a entrada no financeiro.
   * Um pedido só pode ser confirmado uma vez.
   */
  public VendaResumo confirmarPedido(Long id, Dados.Confirmar d, Usuario usuarioLogado) {
    Pedido pedido =
        pedidos.buscarComBloqueio(id).orElseThrow(() -> CadastroService.ausente("Pedido"));
    if (!pedido.status.equals("ABERTO")) throw conflito("Pedido já confirmado ou cancelado");

    Venda v = novaVenda(pedido.cliente, usuarioLogado, d.formaPagamento(), d.canal());
    v.pedido = pedido; // liga a venda ao pedido de origem
    vendas.save(v);

    // Ordena por produto para travar sempre na mesma ordem (evita deadlock).
    List<ItemPedido> itens = new ArrayList<>(itensPedido.findByPedidoId(id));
    itens.sort(Comparator.comparing(x -> x.produto.id));
    for (ItemPedido i : itens) {
      Produto p = bloquear(i.produto.id);
      baixar(p, i.quantidade);
      adicionar(v, p, i.quantidade, i.precoUnitario); // usa o preço que estava no pedido
    }
    pedido.status = "CONCLUIDO";
    concluir(v, pedido.frete);
    return verVenda(v.id);
  }

  // ======================================================================
  // VENDAS
  // ======================================================================

  /** VENDA DIRETA (balcão, sem pedido): baixa o estoque e cria a entrada no financeiro. */
  public VendaResumo criarVenda(Dados.Venda d, Usuario usuarioLogado) {
    Venda v = novaVenda(cadastros.cliente(d.clienteId()), usuarioLogado, d.formaPagamento(), d.canal());
    vendas.save(v);
    for (var e : agrupar(d.itens()).entrySet()) {
      Produto p = bloquear(e.getKey()); // trava o produto e lê o estoque atual
      baixar(p, e.getValue()); // confere e diminui o estoque
      adicionar(v, p, e.getValue(), p.preco); // cria o item com o preço atual do produto
    }
    concluir(v, BigDecimal.ZERO); // sem frete na venda direta
    return verVenda(v.id);
  }

  /**
   * Cancela uma venda CONCLUIDA: devolve as peças ao estoque, cancela o lançamento financeiro e,
   * se a venda veio de um pedido, cancela também o pedido. Não dá para cancelar duas vezes.
   * (O reembolso ao cliente é feito fora da API.)
   */
  public VendaResumo cancelarVenda(Long id) {
    Venda v = vendas.buscarComBloqueio(id).orElseThrow(() -> CadastroService.ausente("Venda"));
    if (v.status.equals("CANCELADA")) throw conflito("Venda já cancelada");

    List<ItemVenda> itens = new ArrayList<>(itensVenda.findByVendaId(id));
    itens.sort(Comparator.comparing(i -> i.produto.id));
    for (ItemVenda i : itens) {
      Produto p = bloquear(i.produto.id);
      p.quantidadeEstoque = Math.addExact(p.quantidadeEstoque, i.quantidade); // devolve ao estoque
    }
    v.status = "CANCELADA";
    for (Financeiro f : financeiro.findByVendaId(id)) {
      f.status = "CANCELADO";
      f.dataPagamento = null;
    }
    if (v.pedido != null) v.pedido.status = "CANCELADO";
    return verVenda(id);
  }

  // ======================================================================
  // PASSOS COMPARTILHADOS PELAS VENDAS
  // ======================================================================

  // Cria o objeto Venda com os dados básicos (ainda sem itens). O total começa em zero.
  private Venda novaVenda(Cliente c, Usuario u, String pagamento, String canal) {
    Venda v = new Venda();
    v.cliente = c;
    v.usuario = u;
    v.data = LocalDateTime.now();
    v.status = "CONCLUIDA";
    v.formaPagamento = pagamento;
    v.canal = canal;
    v.total = BigDecimal.ZERO;
    return v;
  }

  // Diminui o estoque. Se o produto está indisponível ou não tem peças suficientes, dá erro 409
  // e a transação inteira é desfeita.
  private void baixar(Produto p, int quantidade) {
    if (!Boolean.TRUE.equals(p.disponivel) || p.quantidadeEstoque < quantidade)
      throw conflito("Estoque insuficiente ou produto indisponível: " + p.nome);
    p.quantidadeEstoque -= quantidade;
  }

  // Cria o ItemVenda e soma (preço x quantidade) ao total da venda.
  private void adicionar(Venda v, Produto p, int quantidade, BigDecimal preco) {
    ItemVenda i = new ItemVenda();
    i.venda = v;
    i.produto = p;
    i.quantidade = quantidade;
    i.precoUnitario = preco;
    itensVenda.save(i);
    v.total = v.total.add(preco.multiply(BigDecimal.valueOf(quantidade)));
  }

  // Último passo da venda: soma o frete ao total e cria a ENTRADA já paga no financeiro.
  private void concluir(Venda v, BigDecimal frete) {
    v.total = v.total.add(frete);
    Financeiro f = new Financeiro();
    f.venda = v;
    f.usuario = v.usuario;
    f.descricao = "Venda #" + v.id;
    f.categoria = "VENDAS";
    f.tipo = "ENTRADA";
    f.valor = v.total;
    f.dataVencimento = LocalDate.now();
    f.dataPagamento = LocalDate.now();
    f.status = "PAGO";
    financeiro.save(f);
  }
}
