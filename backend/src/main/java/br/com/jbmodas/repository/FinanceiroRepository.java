package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.Financeiro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela financeiro. */
public interface FinanceiroRepository extends JpaRepository<Financeiro, Long> {

  // SELECT * FROM financeiro WHERE venda_id = ?  (lançamentos gerados por uma venda)
  List<Financeiro> findByVendaId(Long id);
}
