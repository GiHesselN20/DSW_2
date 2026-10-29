package br.com.jbmodas.service;

import br.com.jbmodas.model.entity.Financeiro;
import br.com.jbmodas.model.entity.Usuario;
import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.repository.FinanceiroRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lançamentos financeiros feitos À MÃO (aluguel, contas...).
 * As entradas geradas por vendas são criadas pelo ComercialService e NÃO podem ser
 * editadas nem excluídas aqui: elas só mudam quando a venda é cancelada.
 */
@Service
@Transactional
public class FinanceiroService {

  private final FinanceiroRepository repo;

  public FinanceiroService(FinanceiroRepository repo) {
    this.repo = repo;
  }

  /** Formato devolvido nas respostas: só ids (vendaId, usuarioId) em vez dos objetos inteiros. */
  public record Resumo(
      Long id,
      Long vendaId,
      Long usuarioId,
      String descricao,
      String categoria,
      String tipo,
      BigDecimal valor,
      LocalDate dataVencimento,
      LocalDate dataPagamento,
      String status) {}

  // Converte a entidade Financeiro no Resumo acima.
  private Resumo dto(Financeiro f) {
    return new Resumo(
        f.id,
        f.venda == null ? null : f.venda.id,
        f.usuario.id,
        f.descricao,
        f.categoria,
        f.tipo,
        f.valor,
        f.dataVencimento,
        f.dataPagamento,
        f.status);
  }

  public Page<Resumo> listar(Pageable pagina) {
    return repo.findAll(pagina).map(this::dto);
  }

  private Financeiro buscar(Long id) {
    return repo.findById(id).orElseThrow(() -> CadastroService.ausente("Lançamento"));
  }

  public Resumo ver(Long id) {
    return dto(buscar(id));
  }

  /**
   * Cria (id == null) ou atualiza um lançamento manual. O usuário responsável é o que está logado.
   * Regra: dataPagamento só pode existir quando o status for PAGO (e é obrigatória nesse caso).
   */
  public Resumo salvar(Long id, Dados.Financeiro d, Usuario usuarioLogado) {
    Financeiro f = id == null ? new Financeiro() : buscar(id);
    if (f.venda != null)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Lançamentos de vendas são controlados pela venda");
    if (d.status().equals("PAGO") && d.dataPagamento() == null
        || !d.status().equals("PAGO") && d.dataPagamento() != null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe dataPagamento somente para status PAGO");
    f.usuario = usuarioLogado;
    f.descricao = d.descricao();
    f.categoria = d.categoria();
    f.tipo = d.tipo();
    f.valor = d.valor();
    f.dataVencimento = d.dataVencimento();
    f.dataPagamento = d.dataPagamento();
    f.status = d.status();
    return dto(repo.save(f));
  }

  /** Exclui um lançamento manual. Os de venda são protegidos: cancele a venda para estornar. */
  public void excluir(Long id) {
    Financeiro f = buscar(id);
    if (f.venda != null)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Cancele a venda para estornar este lançamento");
    repo.delete(f);
  }
}
