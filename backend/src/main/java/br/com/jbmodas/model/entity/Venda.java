package br.com.jbmodas.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabela "venda": venda realizada e paga.
 * Registrar uma venda baixa o estoque e cria uma ENTRADA no financeiro (veja ComercialService).
 * Status: CONCLUIDA -> CANCELADA. As peças ficam na tabela Item_Venda (classe ItemVenda).
 */
@Entity
@Table(name = "venda")
public class Venda {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_venda")
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "cliente_id")
  public Cliente cliente;

  // Usuário que fez a venda (vem do token de login).
  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  @Column(name = "data")
  public LocalDateTime data;

  @Column(name = "status")
  public String status;

  // Soma dos itens (+ frete, se veio de um pedido). Calculado pela API, nunca enviado pelo cliente.
  @Column(name = "total", precision = 12, scale = 2)
  public BigDecimal total;

  @Column(name = "forma_pagamento")
  public String formaPagamento; // PIX, DEBITO ou CREDITO

  // Pedido de origem. Fica NULL em venda direta (balcão).
  // unique = um mesmo pedido não pode gerar duas vendas.
  @OneToOne
  @JoinColumn(name = "pedido_id", unique = true)
  public Pedido pedido;

  @Column(name = "canal")
  public String canal; // LOJA, WHATSAPP ou INSTAGRAM
}
