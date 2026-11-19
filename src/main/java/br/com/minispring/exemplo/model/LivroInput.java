package br.com.minispring.exemplo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * DTO de entrada separado do modelo: o cliente não escolhe o ID.
 * As anotações são do Bean Validation (jakarta.validation) e são verificadas pelo Spring quando o
 * controller usa @Valid, tanto no POST quanto no PUT. Antes, quem as verificava era o Validator do Mini Spring.
 * Falha de validação gera MethodArgumentNotValidException, traduzida para 400 pelo ApiExceptionHandler.
 */
public record LivroInput(
        @NotBlank(message = "não pode estar em branco") String titulo,
        @Positive(message = "deve ser positivo") int numeroPaginas) {}
