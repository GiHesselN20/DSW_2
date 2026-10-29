package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.Venda;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso à tabela venda (métodos básicos herdados de JpaRepository). */
public interface VendaRepository extends JpaRepository<Venda, Long> {

  /** Busca a venda e trava a linha no banco: impede cancelar a mesma venda duas vezes ao mesmo tempo. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Venda p where p.id = :id")
  Optional<Venda> buscarComBloqueio(@Param("id") Long id);
}
