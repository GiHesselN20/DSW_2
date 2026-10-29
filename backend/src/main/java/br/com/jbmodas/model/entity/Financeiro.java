package br.com.jbmodas.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tabela "financeiro": entradas e saídas de dinheiro.
 *  - Cada venda gera uma ENTRADA automática (campo venda preenchido).
 *  - Despesas como aluguel são lançadas à mão (campo venda fica NULL).
 */
@Entity
@Table(name = "financeiro")
public class Financeiro {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_financeiro")
  public Long id;

  // Opcional: só existe quando o lançamento veio de uma venda.
  @ManyToOne(optional = true)
  @JoinColumn(name = "venda_id")
  public Venda venda;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  @Column(name = "descricao")
  public String descricao;

  @Column(name = "categoria")
  public String categoria; // ex.: VENDAS, DESPESAS_FIXAS

  @Column(name = "tipo")
  public String tipo; // ENTRADA ou SAIDA

  @Column(name = "valor", precision = 12, scale = 2)
  public BigDecimal valor;

  @Column(name = "data_vencimento")
  public LocalDate dataVencimento;

  // Fica NULL enquanto o lançamento não estiver pago.
  @Column(name = "data_pagamento")
  public LocalDate dataPagamento;

  @Column(name = "status")
  public String status; // PENDENTE, PAGO ou CANCELADO
}
