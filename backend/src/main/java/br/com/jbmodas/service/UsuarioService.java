package br.com.jbmodas.service;

import br.com.jbmodas.model.entity.Usuario;
import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.repository.UsuarioRepository;
import br.com.jbmodas.util.JwtUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Usuários da loja e LOGIN. Aqui nasce o token JWT (veja util/WebConfig.java e util/JwtUtil.java para a configuração do token).
 */
@Service
public class UsuarioService {

  private final UsuarioRepository repo;
  private final PasswordEncoder senhas; // BCrypt: criptografa e confere senhas
  private final JwtUtil jwt; // cria tokens JWT

  public UsuarioService(
      UsuarioRepository repo,
      PasswordEncoder senhas,
      JwtUtil jwt) {
    this.repo = repo;
    this.senhas = senhas;
    this.jwt = jwt;
  }

  /** Versão do usuário que pode sair nas respostas: sem a senha. */
  public record Publico(Long id, String nome, String email) {}

  public Publico publico(Usuario u) {
    return new Publico(u.id, u.nome, u.email);
  }

  /** Cadastra um usuário. A senha é criptografada com BCrypt antes de ir para o banco. */
  @Transactional
  public Publico criar(Dados.Usuario d) {
    // O BCrypt só considera os primeiros 72 bytes da senha, então senhas maiores são recusadas.
    if (d.senha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Senha excede o limite de 72 bytes em UTF-8");
    String email = d.email().trim().toLowerCase(Locale.ROOT); // e-mail sempre minúsculo
    if (repo.findByEmail(email).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
    Usuario u = new Usuario();
    u.nome = d.nome();
    u.email = email;
    u.senha = senhas.encode(d.senha());
    return publico(repo.save(u));
  }

  /**
   * LOGIN: confere e-mail + senha e, se estiverem certos, devolve o token.
   * A mensagem de erro é a mesma para e-mail inexistente e senha errada (não revela qual falhou).
   */
  public Map<String, Object> login(Dados.Login d) {
    if (d.senha().getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");

    Usuario u =
        repo.findByEmail(d.email().trim().toLowerCase(Locale.ROOT))
            .filter(x -> senhas.matches(d.senha(), x.senha)) // compara a senha digitada com o hash
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

    String token = jwt.gerarToken(u.id);
    return Map.of(
        "token", token,
        "tipo", "Bearer",
        "expiraEmSegundos", jwt.expiraEmSegundos(),
        "usuario", publico(u));
  }

  /**
   * Descobre QUEM está logado. O Spring já validou o token; aqui lemos o id guardado nele
   * (campo "subject") e buscamos o usuário no banco. Se ele foi apagado, devolve 401.
   */
  public Usuario atual(Jwt token) {
    return repo.findById(Long.valueOf(token.getSubject()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inválido"));
  }

  public List<Publico> listar() {
    return repo.findAll().stream().map(this::publico).toList();
  }
}
