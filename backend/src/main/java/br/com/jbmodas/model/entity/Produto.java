package br.com.jbmodas.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Tabela "produto": uma peça de roupa e o seu estoque.
 * Cada combinação de tamanho + cor é um produto separado
 * (ex.: "Conjunto verde" M e "Conjunto verde" G são dois registros).
 */
@Entity
@Table(name = "produto")
public class Produto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_produto")
  public Long id;

  @Column(name = "nome")
  public String nome;

  @Column(name = "categoria")
  public String categoria; // FITNESS ou PRAIA

  @Column(name = "tamanho")
  public String tamanho;

  @Column(name = "cor")
  public String cor;

  // BigDecimal é o tipo correto para dinheiro (double perde centavos).
  @Column(name = "preco", precision = 12, scale = 2)
  public BigDecimal preco;

  // Quantas peças existem. Diminui quando há uma venda e volta quando a venda é cancelada.
  @Column(name = "quantidade_estoque")
  public Integer quantidadeEstoque;

  // false = a peça não está mais à venda (mas continua no histórico das vendas antigas).
  @Column(name = "disponivel")
  public Boolean disponivel;
}
