package br.com.jbmodas;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.jbmodas.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ProdutoRepository produtos;
  @Autowired FinanceiroRepository financeiro;
  @Autowired VendaRepository vendas;
  @Autowired UsuarioRepository usuarios;
  @Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
  String token;

  @BeforeEach
  void entrar() throws Exception {
    token =
        json.readTree(
                mvc.perform(
                        post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"email\":\"admin@teste.local\",\"senha\":\"SenhaTeste123\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("token")
            .asText();
  }

  JsonNode criar(String url, String body) throws Exception {
    return json.readTree(
        mvc.perform(
                post(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  long cliente() throws Exception {
    return criar(
            "/clientes",
            "{\"nome\":\"Cliente teste\",\"telefone\":\"89994569750\",\"endereco\":\"Picos\"}")
        .get("id")
        .asLong();
  }

  long produto(int estoque) throws Exception {
    return criar(
            "/produtos",
            "{\"nome\":\"Conjunto\",\"categoria\":\"FITNESS\",\"tamanho\":\"M\",\"cor\":\"Verde\",\"preco\":100.00,\"quantidadeEstoque\":"
                + estoque
                + ",\"disponivel\":true}")
        .get("id")
        .asLong();
  }

  String venda(long c, long p, int q) {
    return "{\"clienteId\":"
        + c
        + ",\"formaPagamento\":\"PIX\",\"canal\":\"LOJA\",\"itens\":[{\"produtoId\":"
        + p
        + ",\"quantidade\":"
        + q
        + "}]}";
  }

  @Test
  void protegeRotasESenha() throws Exception {
    mvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
    mvc.perform(get("/clientes").header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@teste.local\",\"senha\":\"errada\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.senha").doesNotExist());
    assertThat(usuarios.findByEmail("admin@teste.local").orElseThrow().senha).startsWith("$2a$");
  }

  @Test
  void vendaBaixaEstoqueEGeraFinanceiroECancelamentoEstorna() throws Exception {
    long c = cliente(), p = produto(5);
    var v = criar("/vendas", venda(c, p, 2));
    long id = v.get("id").asLong();
    assertThat(v.get("total").decimalValue()).isEqualByComparingTo("200.00");
    assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isEqualTo(3);
    assertThat(financeiro.findByVendaId(id)).hasSize(1);
    mvc.perform(post("/vendas/" + id + "/cancelar").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
    assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isEqualTo(5);
    assertThat(financeiro.findByVendaId(id).get(0).status).isEqualTo("CANCELADO");
    mvc.perform(post("/vendas/" + id + "/cancelar").header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
    assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isEqualTo(5);
  }

  @Test
  void estoqueInsuficienteDesfazVendaInteira() throws Exception {
    long c = cliente(), p = produto(1);
    long antes = vendas.count();
    mvc.perform(
            post("/vendas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(venda(c, p, 2)))
        .andExpect(status().isConflict());
    assertThat(vendas.count()).isEqualTo(antes);
    assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isEqualTo(1);
  }

  @Test
  void pedidoConverteUmaVezComFrete() throws Exception {
    long c = cliente(), p = produto(5);
    var pe =
        criar(
            "/pedidos",
            "{\"clienteId\":"
                + c
                + ",\"tipoEntrega\":\"ENTREGA\",\"enderecoEntrega\":\"Rua teste"
                + " 10\",\"frete\":10,\"itens\":[{\"produtoId\":"
                + p
                + ",\"quantidade\":2}]}");
    long id = pe.get("id").asLong();
    assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isEqualTo(5);
    var v =
        criar(
            "/pedidos/" + id + "/confirmar", "{\"formaPagamento\":\"PIX\",\"canal\":\"WHATSAPP\"}");
    assertThat(v.get("total").decimalValue()).isEqualByComparingTo("210");
    mvc.perform(
            post("/pedidos/" + id + "/confirmar")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formaPagamento\":\"PIX\",\"canal\":\"WHATSAPP\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void validaCamposEProtegeLancamentoAutomatico() throws Exception {
    mvc.perform(
            post("/produtos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    long c = cliente(), p = produto(2);
    var v = criar("/vendas", venda(c, p, 1));
    long f = financeiro.findByVendaId(v.get("id").asLong()).get(0).id;
    mvc.perform(delete("/financeiro/" + f).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
    mvc.perform(delete("/produtos/" + p).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
  }

  @Test
  void validaDatasFinanceiras() throws Exception {
    mvc.perform(
            post("/financeiro")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"descricao\":\"Aluguel\",\"categoria\":\"FIXO\",\"tipo\":\"SAIDA\",\"valor\":100,\"dataVencimento\":\"2026-10-28\",\"status\":\"PAGO\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/relatorios/vendas?inicio=2026-10-28&fim=2026-10-01")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void tokenExpiradoOuAssinadoPorOutraChaveNaoEntra() throws Exception {
    var agora = java.time.Instant.now();
    var claims =
        org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("jb-modas-api")
            .subject("1")
            .issuedAt(agora.minusSeconds(500))
            .expiresAt(agora.minusSeconds(120))
            .build();
    var header =
        org.springframework.security.oauth2.jwt.JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
            .build();
    String expirado =
        encoder
            .encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims))
            .getTokenValue();
    mvc.perform(get("/clientes").header("Authorization", "Bearer " + expirado))
        .andExpect(status().isUnauthorized());
    var outro =
        new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
                new javax.crypto.spec.SecretKeySpec(
                    "outra-chave-totalmente-diferente-com-32-bytes"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256")));
    var claims2 =
        org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("jb-modas-api")
            .subject("1")
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(3600))
            .build();
    String falso =
        outro
            .encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims2))
            .getTokenValue();
    mvc.perform(get("/clientes").header("Authorization", "Bearer " + falso))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void falhaNoSegundoItemRevertePrimeiroEFinanceiro() throws Exception {
    long c = cliente(), p1 = produto(5), p2 = produto(0);
    long antes = vendas.count(), fin = financeiro.count();
    String body =
        "{\"clienteId\":"
            + c
            + ",\"formaPagamento\":\"PIX\",\"canal\":\"LOJA\",\"itens\":[{\"produtoId\":"
            + p1
            + ",\"quantidade\":2},{\"produtoId\":"
            + p2
            + ",\"quantidade\":1}]}";
    mvc.perform(
            post("/vendas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
    assertThat(produtos.findById(p1).orElseThrow().quantidadeEstoque).isEqualTo(5);
    assertThat(vendas.count()).isEqualTo(antes);
    assertThat(financeiro.count()).isEqualTo(fin);
  }

  @Test
  void vendasConcorrentesNaoUltrapassamEstoque() throws Exception {
    long c = cliente(), p = produto(1);
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    var inicio = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.Callable<Integer> comprar =
        () -> {
          inicio.await();
          return mvc.perform(
                  post("/vendas")
                      .header("Authorization", "Bearer " + token)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(venda(c, p, 1)))
              .andReturn()
              .getResponse()
              .getStatus();
        };
    try {
      var a = executor.submit(comprar);
      var b = executor.submit(comprar);
      inicio.countDown();
      assertThat(
              java.util.List.of(
                  a.get(10, java.util.concurrent.TimeUnit.SECONDS),
                  b.get(10, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
      assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isZero();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void pedidosConcorrentesNaoUsamSaldoAntigo() throws Exception {
    long c = cliente(), p = produto(1);
    String pedido =
        "{\"clienteId\":"
            + c
            + ",\"tipoEntrega\":\"RETIRADA\",\"frete\":0,\"itens\":[{\"produtoId\":"
            + p
            + ",\"quantidade\":1}]}";
    long a = criar("/pedidos", pedido).get("id").asLong(),
        b = criar("/pedidos", pedido).get("id").asLong();
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    var inicio = new java.util.concurrent.CountDownLatch(1);
    try {
      var resultados = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
      for (long id : java.util.List.of(a, b))
        resultados.add(
            executor.submit(
                () -> {
                  inicio.await();
                  return mvc.perform(
                          post("/pedidos/" + id + "/confirmar")
                              .header("Authorization", "Bearer " + token)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content("{\"formaPagamento\":\"PIX\",\"canal\":\"LOJA\"}"))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      inicio.countDown();
      assertThat(
              java.util.List.of(
                  resultados.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS),
                  resultados.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
      assertThat(produtos.findById(p).orElseThrow().quantidadeEstoque).isZero();
    } finally {
      executor.shutdownNow();
    }
  }
}
