package br.com.jbmodas.model.entity;

import jakarta.persistence.*;

/**
 * Tabela "cliente": quem compra na loja.
 *
 * Cada objeto desta classe é UMA linha da tabela. Os campos são públicos de propósito,
 * para o código ficar curto (sem getters/setters). O Hibernate converte os nomes para
 * snake_case: a classe ItemPedido vira a tabela item_pedido.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

  // Chave primária. IDENTITY = o MySQL gera o número sozinho (AUTO_INCREMENT).
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_cliente")
  public Long id;

  @Column(name = "nome")
  public String nome;

  @Column(name = "telefone")
  public String telefone;

  @Column(name = "endereco")
  public String endereco;
}
