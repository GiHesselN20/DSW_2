<<<<<<< HEAD
# JB Modas — API

Backend (sem tela) da loja: clientes, produtos/estoque, pedidos, vendas e financeiro, com login por token (JWT).
Java 17, Spring Boot 3.5.7, Maven, JPA/Hibernate e MySQL. Sem Docker.

## Como rodar

1. Instale JDK 17+ e MySQL 8+ (ligado).
2. Abra `src/main/resources/application.properties` e confira os valores padrão:
   - `DB_USER` / `DB_PASSWORD`: usuário e senha do **seu** MySQL (padrão: `root` / `1234`).
   - `ADMIN_EMAIL` / `ADMIN_PASSWORD`: o primeiro usuário, criado sozinho se a tabela `usuario` estiver vazia
     (padrão: `dona@jbmodas.com` / `troque1234`). **Troque a senha.**
   - `JWT_SECRET`: chave que assina os tokens (32+ caracteres). **Troque antes de publicar.**
3. Rode a classe `JbModasApplication` na IDE, ou no terminal: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
4. Teste: `GET http://localhost:8080/health` deve responder `{"status":"ok"}`.

Cada valor do `application.properties` está no formato `${NOME:padrão}`: se existir uma variável de ambiente com
esse NOME ela é usada; senão vale o padrão. Ou seja, dá para editar o arquivo ou configurar variáveis na IDE.

O Hibernate cria as tabelas sozinho (`ddl-auto=update`). Use um banco novo para este projeto.

## Como o login (token) funciona

1. `POST /auth/login` com `{"email":"...","senha":"..."}`. A API confere a senha (BCrypt) e devolve um `token`.
2. Em todas as outras chamadas envie o cabeçalho `Authorization: Bearer SEU_TOKEN`.
3. Sem token, com token vencido (60 min) ou adulterado: erro **401**.
4. O usuário que cria pedidos, vendas e lançamentos é descoberto **pelo token** (não vem no JSON).

O código disso está em `util/Seguranca.java` (regras) e `service/UsuarioService.java` (criação do token), ambos comentados.

## Testar no Postman

Importe `postman/JB-Modas.postman_collection.json`, preencha `email` e `senha` nas variáveis da coleção e execute
as requisições na ordem numerada. O login salva o token automaticamente.

## Rotas (todas exigem token, exceto `/auth/login` e `/health`)

| Área | Rotas |
|---|---|
| Acesso | `POST /auth/login`, `GET /auth/me` |
| Usuários | `GET /usuarios`, `POST /usuarios` |
| Clientes | `GET /clientes`, `GET /clientes/{id}`, `POST /clientes`, `PUT /clientes/{id}`, `DELETE /clientes/{id}` |
| Produtos/estoque | `GET /produtos`, `GET /produtos/{id}`, `POST /produtos`, `PUT /produtos/{id}`, `DELETE /produtos/{id}` |
| Pedidos | `GET /pedidos`, `GET /pedidos/{id}`, `POST /pedidos`, `POST /pedidos/{id}/confirmar`, `POST /pedidos/{id}/cancelar` |
| Vendas | `GET /vendas`, `GET /vendas/{id}`, `POST /vendas`, `POST /vendas/{id}/cancelar` |
| Financeiro | `GET /financeiro`, `GET /financeiro/{id}`, `POST /financeiro`, `PUT /financeiro/{id}`, `DELETE /financeiro/{id}` |
| Painel | `GET /dashboard` |
| Relatórios | `GET /relatorios/vendas?inicio=2026-10-01&fim=2026-10-31`, `GET /relatorios/financeiro?inicio=...&fim=...`, `GET /relatorios/estoque?limite=5` |

Listagens aceitam `?pagina=0&tamanho=20` (máximo 100 por página).

## Exemplos de JSON

Cliente: `{"nome":"Maria Silva","telefone":"89999999999","endereco":"Picos - PI"}`

Produto (cada tamanho/cor é um registro):
`{"nome":"Conjunto verde","categoria":"FITNESS","tamanho":"M","cor":"Verde","preco":129.90,"quantidadeEstoque":10,"disponivel":true}`

Pedido: `{"clienteId":1,"tipoEntrega":"RETIRADA","frete":0,"observacoes":"Separar tamanho M","itens":[{"produtoId":1,"quantidade":2}]}`
(para `ENTREGA`, informe `enderecoEntrega`; `RETIRADA` não tem frete)

Confirmar pedido (`POST /pedidos/1/confirmar`): `{"formaPagamento":"PIX","canal":"WHATSAPP"}`

Venda direta: `{"clienteId":1,"formaPagamento":"PIX","canal":"LOJA","itens":[{"produtoId":1,"quantidade":2}]}`

Lançamento manual: `{"descricao":"Aluguel","categoria":"DESPESAS_FIXAS","tipo":"SAIDA","valor":900,"dataVencimento":"2026-10-28","dataPagamento":"2026-10-28","status":"PAGO"}`

Usuário novo: `{"nome":"Atendente","email":"atendente@jbmodas.com","senha":"uma_senha_forte"}`

Valores aceitos: categoria `FITNESS|PRAIA`; pagamento `PIX|DEBITO|CREDITO`; canal `LOJA|WHATSAPP|INSTAGRAM`;
tipo financeiro `ENTRADA|SAIDA`; status financeiro `PENDENTE|PAGO|CANCELADO` (`dataPagamento` só quando PAGO).

## Regras da loja

- Preços e totais são calculados no servidor; o preço de cada peça fica gravado no pedido/venda.
- Pedido ABERTO **não** mexe no estoque. Confirmar (ou vender direto) baixa o estoque e cria uma entrada paga no financeiro,
  tudo na mesma transação: se faltar estoque em qualquer item, nada é gravado.
- Um pedido gera no máximo uma venda.
- Cancelar venda devolve o estoque, cancela o lançamento financeiro e o pedido de origem. Não cancela duas vezes.
- Lançamentos criados por vendas não podem ser editados/excluídos à mão.
- Nada é apagado em cascata: cliente/produto com histórico não pode ser excluído (409). Marque o produto como `disponivel:false`.
- O saldo do financeiro não é lucro: o sistema não guarda o custo das peças.

## Organização do código

```text
src/main/java/br/com/jbmodas/
  controller/      rotas HTTP (recebem o JSON e chamam o service)
  service/         regras da loja (estoque, vendas, login, relatórios)
  repository/      acesso ao banco (sem escrever SQL)
  model/entity/    as 8 tabelas do diagrama
  model/request/   Dados.java: formato e validação dos JSONs recebidos
  util/            segurança/token, tratamento de erros, primeiro usuário, paginação
src/test/          testes de integração (H2 em memória, não precisam do MySQL)
postman/           requisições prontas
```

Caminho de uma chamada: **Controller -> Service -> Repository -> MySQL**.

## Testes

`./mvnw test` (Windows: `mvnw.cmd test`). Usam H2 em memória e as configurações de `src/test/resources/application.properties`.
=======
# ModaJB_Projeto
>>>>>>> 3255e0d28bb114a3c8d2b7238eae377f6a9f48f3
