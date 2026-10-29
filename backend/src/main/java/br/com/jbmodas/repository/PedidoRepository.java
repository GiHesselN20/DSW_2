package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso à tabela pedido (métodos básicos herdados de JpaRepository). */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

  /**
   * Busca o pedido e trava a linha no banco até o fim da transação.
   * Evita que o mesmo pedido seja confirmado (ou cancelado) duas vezes ao mesmo tempo.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pedido p where p.id = :id")
  Optional<Pedido> buscarComBloqueio(@Param("id") Long id);
}
