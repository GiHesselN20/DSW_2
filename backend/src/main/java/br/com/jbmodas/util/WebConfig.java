package br.com.jbmodas.util;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuração web e de segurança da API JB Modas.
 *
 * COMO O LOGIN FUNCIONA:
 *  1. O usuário envia e-mail e senha para POST /auth/login.
 *  2. Se estiverem certos, a API cria um TOKEN (um texto assinado com a nossa chave secreta)
 *     contendo o id do usuário e a hora em que expira. Quem cria é o "encoder" abaixo.
 *  3. Nas próximas chamadas, o front envia o cabeçalho  Authorization: Bearer <token>.
 *  4. Antes de chegar ao controller, o Spring Security confere a assinatura e a validade do token
 *     (o "decoder" abaixo). Token ausente, adulterado ou vencido = erro 401.
 *
 * Cada método @Bean abaixo cria uma peça que o Spring usa automaticamente.
 */
@Configuration
public class WebConfig {

  /** Chave secreta usada para assinar e conferir os tokens (vem de app.jwt.secret). */
  @Bean
  SecretKeySpec chave(@Value("${app.jwt.secret}") String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET precisa de pelo menos 32 bytes");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  /** CRIA tokens (usado no login, em UsuarioService). */
  @Bean
  JwtEncoder encoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  /** CONFERE tokens recebidos: assinatura, validade e emissor ("jb-modas-api"). */
  @Bean
  JwtDecoder decoder(SecretKeySpec key) {
    var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("jb-modas-api"));
    return decoder;
  }

  /** Criptografa e confere senhas com BCrypt: no banco fica só o hash, nunca a senha real. */
  @Bean
  PasswordEncoder senhas() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Regras de acesso:
   *  - /auth/login, /health e /error são públicos;
   *  - QUALQUER outra rota exige um token válido;
   *  - STATELESS = o servidor não guarda sessão; o token é a única prova de quem você é.
   */
  @Bean
  SecurityFilterChain filtros(HttpSecurity http) throws Exception {
    return http.csrf(c -> c.disable())
        .cors(c -> {})
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/auth/login", "/health", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> {}))
        .build();
  }

  /** CORS: libera o front-end (app.cors.origin) a chamar a API pelo navegador. */
  @Bean
  CorsConfigurationSource cors(@Value("${app.cors.origin}") String origin) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(origin));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
