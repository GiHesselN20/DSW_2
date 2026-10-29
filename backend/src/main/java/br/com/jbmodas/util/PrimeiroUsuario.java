package br.com.jbmodas.util;

import br.com.jbmodas.model.request.Dados;
import br.com.jbmodas.repository.UsuarioRepository;
import br.com.jbmodas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Roda UMA vez sempre que a API inicia. Se a tabela usuario estiver vazia, cria o primeiro usuário
 * com o e-mail e a senha de app.admin.email / app.admin.password (application.properties).
 * Sem isso ninguém conseguiria fazer o primeiro login, já que todas as rotas exigem token.
 */
@Component
public class PrimeiroUsuario implements CommandLineRunner {

  private final UsuarioRepository usuarios;
  private final UsuarioService service;
  private final String email;
  private final String senha;

  public PrimeiroUsuario(
      UsuarioRepository usuarios,
      UsuarioService service,
      @Value("${app.admin.email}") String email,
      @Value("${app.admin.password}") String senha) {
    this.usuarios = usuarios;
    this.service = service;
    this.email = email;
    this.senha = senha;
  }

  // Executado automaticamente pelo Spring ao iniciar (por causa do CommandLineRunner).
  @Override
  public void run(String... args) {
    if (usuarios.count() == 0 && !email.isBlank() && !senha.isBlank()) {
      if (!email.contains("@") || senha.length() < 8 || senha.length() > 60)
        throw new IllegalArgumentException(
            "Confira ADMIN_EMAIL e ADMIN_PASSWORD (8 a 60 caracteres)");
      service.criar(new Dados.Usuario("Administradora", email, senha));
    }
  }
}
