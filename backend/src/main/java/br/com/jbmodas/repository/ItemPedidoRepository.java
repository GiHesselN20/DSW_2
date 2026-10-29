package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.ItemPedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela item_pedido. */
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

  // SELECT * FROM item_pedido WHERE pedido_id = ?  (todas as peças de um pedido)
  List<ItemPedido> findByPedidoId(Long id);

  void deleteByPedidoId(Long id);
}
