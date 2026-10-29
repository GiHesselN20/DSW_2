package br.com.jbmodas.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Tabela "usuario": quem trabalha na loja e faz login na API.
 * O usuário que cria um pedido, uma venda ou um lançamento fica registrado nele.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Column(name = "nome")
  public String nome;

  // unique = não existem dois usuários com o mesmo e-mail; nullable = false = obrigatório.
  @Column(name = "email", unique = true, nullable = false)
  public String email;

  // Guarda o HASH BCrypt da senha (nunca a senha real). @JsonIgnore = nunca aparece no JSON.
  @JsonIgnore
  @Column(name = "senha", length = 255)
  public String senha;
}
