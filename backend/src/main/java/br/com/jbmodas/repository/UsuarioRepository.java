package br.com.jbmodas.repository;

import br.com.jbmodas.model.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela usuario (métodos básicos herdados de JpaRepository: findAll, save, count...). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  // O Spring lê o NOME do método e monta o SQL sozinho: SELECT * FROM usuario WHERE email = ?
  // Optional = pode vir vazio quando o e-mail não existe. Usado no login.
  Optional<Usuario> findByEmail(String email);
}
