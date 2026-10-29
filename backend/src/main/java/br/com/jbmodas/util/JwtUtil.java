package br.com.jbmodas.util;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

/**
 * Cria tokens JWT para usuários autenticados.
 * A validação de tokens recebidos é feita pelo Spring Security em WebConfig.
 */
@Component
public class JwtUtil {
  private final JwtEncoder encoder;
  private final long minutos;

  public JwtUtil(JwtEncoder encoder, @Value("${app.jwt.minutes}") long minutos) {
    this.encoder = encoder;
    this.minutos = minutos;
    if (minutos < 1) {
      throw new IllegalArgumentException("JWT_MINUTES inválido");
    }
  }

  public String gerarToken(Long usuarioId) {
    Instant agora = Instant.now();
    var claims = JwtClaimsSet.builder()
        .issuer("jb-modas-api")
        .subject(usuarioId.toString())
        .issuedAt(agora)
        .expiresAt(agora.plusSeconds(expiraEmSegundos()))
        .build();

    return encoder.encode(
        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public long expiraEmSegundos() {
    return minutos * 60;
  }
}
