package br.com.jbmodas.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabela "pedido": encomenda de um cliente. Um pedido NÃO baixa estoque.
 * O estoque só baixa quando o pedido é confirmado e vira uma Venda.
 * Status: ABERTO -> CONCLUIDO (virou venda) ou CANCELADO.
 * As peças do pedido ficam na tabela Item_Pedido (classe ItemPedido).
 */
@Entity
@Table(name = "pedido")
public class Pedido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_pedido")
  public Long id;

  // Muitos pedidos -> um cliente. No banco vira a coluna cliente_id (chave estrangeira).
  @ManyToOne(optional = false)
  @JoinColumn(name = "cliente_id")
  public Cliente cliente;

  // Usuário da loja que registrou o pedido (vem do token de login).
  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  @Column(name = "data")
  public LocalDateTime data;

  @Column(name = "status")
  public String status;

  @Column(name = "tipo_entrega")
  public String tipoEntrega; // RETIRADA ou ENTREGA

  @Column(name = "endereco_entrega")
  public String enderecoEntrega;

  @Column(name = "frete", precision = 12, scale = 2)
  public BigDecimal frete;

  @Column(name = "observacoes")
  public String observacoes;
}
