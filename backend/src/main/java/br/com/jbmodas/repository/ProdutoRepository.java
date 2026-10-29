package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.Produto;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso à tabela produto (métodos básicos herdados de JpaRepository). */
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

  /**
   * Busca o produto e TRAVA a linha no banco (SELECT ... FOR UPDATE) até o fim da transação.
   * Serve para duas vendas simultâneas não venderem a mesma última peça: a segunda espera
   * a primeira terminar e só então enxerga o estoque já atualizado.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Produto p where p.id = :id")
  Optional<Produto> buscarComBloqueio(@Param("id") Long id);
}
