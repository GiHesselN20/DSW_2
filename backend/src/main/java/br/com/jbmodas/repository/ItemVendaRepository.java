package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.ItemVenda;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela item_venda. */
public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

  // SELECT * FROM item_venda WHERE venda_id = ?  (todas as peças de uma venda)
  List<ItemVenda> findByVendaId(Long id);
}
