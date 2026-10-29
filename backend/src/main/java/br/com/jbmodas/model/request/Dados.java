package br.com.jbmodas.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Formato dos JSONs que a API ACEITA receber, com as validações de cada campo.
 *
 * Por que não usar as entidades direto? Assim quem chama a API não consegue enviar
 * por conta própria o id, o total, o status ou o usuário responsável: isso é decidido aqui dentro.
 *
 * Cada "record" é uma classe imutável só com dados. As anotações validam o JSON automaticamente
 * (por causa do @Valid nos controllers). Se algo estiver errado, a API responde 400 com os campos
 * com problema. Legenda:
 *   @NotBlank = texto obrigatório     @NotNull = obrigatório      @Size = tamanho do texto
 *   @Min / @Positive / @DecimalMin = valor mínimo    @Pattern = só aceita os valores listados
 */
public class Dados {

  /** POST /auth/login */
  public record Login(@NotBlank @Email String email, @NotBlank String senha) {}

  /** POST /usuarios */
  public record Usuario(
      @NotBlank @Size(max = 100) String nome,
      @NotBlank @Email @Size(max = 150) String email,
      @NotBlank @Size(min = 8, max = 60) String senha) {}

  /** POST e PUT /clientes */
  public record Cliente(
      @NotBlank @Size(max = 100) String nome,
      @NotBlank @Size(max = 20) String telefone,
      @Size(max = 150) String endereco) {}

  /** POST e PUT /produtos */
  public record Produto(
      @NotBlank @Size(max = 100) String nome,
      @NotBlank @Pattern(regexp = "FITNESS|PRAIA") String categoria,
      @NotBlank @Size(max = 20) String tamanho,
      @NotBlank @Size(max = 100) String cor,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal preco,
      @NotNull @Min(0) Integer quantidadeEstoque,
      @NotNull Boolean disponivel) {}

  /** Uma linha de pedido ou de venda: qual produto e quantas unidades. */
  public record Item(@NotNull @Positive Long produtoId, @NotNull @Min(1) Integer quantidade) {}

  /** POST /pedidos. A lista de itens usa o record Item acima (@Valid valida cada item). */
  public record Pedido(
      @NotNull @Positive Long clienteId,
      @NotBlank @Pattern(regexp = "RETIRADA|ENTREGA") String tipoEntrega,
      @Size(max = 250) String enderecoEntrega,
      @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal frete,
      @Size(max = 2000) String observacoes,
      @NotEmpty @Size(max = 100) List<@Valid Item> itens) {}

  /** POST /vendas (venda direta, sem pedido). */
  public record Venda(
      @NotNull @Positive Long clienteId,
      @NotBlank @Pattern(regexp = "PIX|DEBITO|CREDITO") String formaPagamento,
      @NotBlank @Pattern(regexp = "LOJA|WHATSAPP|INSTAGRAM") String canal,
      @NotEmpty @Size(max = 100) List<@Valid Item> itens) {}

  /** POST /pedidos/{id}/confirmar: como o pedido foi pago. */
  public record Confirmar(
      @NotBlank @Pattern(regexp = "PIX|DEBITO|CREDITO") String formaPagamento,
      @NotBlank @Pattern(regexp = "LOJA|WHATSAPP|INSTAGRAM") String canal) {}

  /** POST e PUT /financeiro (lançamentos manuais). */
  public record Financeiro(
      @NotBlank @Size(max = 250) String descricao,
      @NotBlank @Size(max = 50) String categoria,
      @NotBlank @Pattern(regexp = "ENTRADA|SAIDA") String tipo,
      @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal valor,
      @NotNull LocalDate dataVencimento,
      LocalDate dataPagamento,
      @NotBlank @Pattern(regexp = "PENDENTE|PAGO|CANCELADO") String status) {}
}
