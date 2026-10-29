package br.com.jbmodas.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Tabela "item_venda": uma linha da venda (qual peça, quantas unidades e a que preço). */
@Entity
@Table(name = "item_venda")
public class ItemVenda {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_item_venda")
  public Long id;

  @ManyToOne(optional = true)
  @JoinColumn(name = "venda_id")
  public Venda venda;

  @ManyToOne(optional = false)
  @JoinColumn(name = "produto_id")
  public Produto produto;

  @Column(name = "quantidade")
  public Integer quantidade;

  // Preço copiado na hora da venda: mudar o preço do produto depois não altera vendas antigas.
  @Column(name = "preco_unitario", precision = 12, scale = 2)
  public BigDecimal precoUnitario;
}
