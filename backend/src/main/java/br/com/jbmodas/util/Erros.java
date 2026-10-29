package br.com.jbmodas.util;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Transforma exceções em respostas JSON organizadas ({"mensagem": "..."}).
 * Vale para todos os controllers: quando um service lança um erro, cai aqui.
 */
@RestControllerAdvice
public class Erros {

  // Erros que NÓS lançamos nos services (404 não encontrado, 409 conflito, 400, 401...).
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> negocio(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("mensagem", e.getReason() == null ? "Requisição inválida" : e.getReason()));
  }

  // JSON que não passou nas validações de Dados.java (@NotBlank, @Min...). Lista os campos errados.
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "mensagem",
                "Confira os campos",
                "erros",
                e.getBindingResult().getFieldErrors().stream()
                    .map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .toList()));
  }

  // O banco recusou: e-mail repetido, ou tentativa de apagar algo que tem histórico ligado.
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflito() {
    return ResponseEntity.status(409)
        .body(Map.of("mensagem", "Registro duplicado ou vinculado a outros dados"));
  }

  // JSON quebrado ou com algum campo que não existe.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<?> json() {
    return ResponseEntity.badRequest()
        .body(Map.of("mensagem", "JSON inválido ou campo desconhecido"));
  }
}
