package br.com.jbmodas.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Tabela "item_pedido": uma linha do pedido (qual peça e quantas unidades). */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_item_pedido")
  public Long id;

  // A que pedido este item pertence (coluna pedido_id).
  @ManyToOne(optional = false)
  @JoinColumn(name = "pedido_id")
  public Pedido pedido;

  // Qual peça foi pedida (coluna produto_id).
  @ManyToOne(optional = false)
  @JoinColumn(name = "produto_id")
  public Produto produto;

  @Column(name = "quantidade")
  public Integer quantidade;

  // Preço da peça NO MOMENTO do pedido. Se o preço do produto mudar depois, este valor não muda.
  @Column(name = "preco_unitario", precision = 12, scale = 2)
  public BigDecimal precoUnitario;
}
