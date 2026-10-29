package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela cliente. Só de estender JpaRepository<Cliente, Long> já ganhamos, sem escrever SQL:
 * findAll() (SELECT *), findById(id), save(obj) (INSERT se o id for vazio, UPDATE se existir),
 * delete(obj), count() e a versão paginada findAll(Pageable).
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {}
