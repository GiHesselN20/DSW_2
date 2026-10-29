package br.com.jbmodas.controller;

import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login, "quem sou eu" e cadastro de usuários.
 *
 * @AuthenticationPrincipal Jwt jwt = o Spring entrega aqui o token JÁ VALIDADO da requisição
 * (cabeçalho Authorization: Bearer ...). Dele sai o id de quem está logado.
 * @Valid = confere o JSON recebido com as regras de Dados.java antes de entrar no método.
 */
@RestController
public class AuthController {

  private final UsuarioService service;

  public AuthController(UsuarioService service) {
    this.service = service;
  }

  /** GET /health (público) -> serve só para testar se a API está no ar. */
  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }

  /**
   * POST /auth/login (público). Corpo: {"email": "...", "senha": "..."}
   * Resposta: token, tipo, expiraEmSegundos e o usuário (sem senha).
   */
  @PostMapping("/auth/login")
  public Map<String, Object> login(@Valid @RequestBody Dados.Login dados) {
    return service.login(dados);
  }

  /** GET /auth/me -> devolve o usuário dono do token enviado. */
  @GetMapping("/auth/me")
  public UsuarioService.Publico me(@AuthenticationPrincipal Jwt jwt) {
    return service.publico(service.atual(jwt));
  }

  /** POST /usuarios -> cadastra outro usuário (só quem está logado pode). Responde 201. */
  @PostMapping("/usuarios")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioService.Publico criar(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Dados.Usuario d) {
    service.atual(jwt); // confirma que o usuário do token ainda existe
    return service.criar(d);
  }

  /** GET /usuarios -> lista os usuários (sem senhas). */
  @GetMapping("/usuarios")
  public List<UsuarioService.Publico> listar(@AuthenticationPrincipal Jwt jwt) {
    service.atual(jwt);
    return service.listar();
  }
}
