package br.com.minispring.exemplo.web;

import br.com.minispring.exemplo.exception.LivroNaoEncontradoException;
import br.com.minispring.exemplo.exception.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Tratamento centralizado de erros: substitui os blocos catch do DispatcherServlet do Mini Spring.
 * Toda exceção lançada por controller/service chega aqui e vira uma resposta JSON com o status adequado.
 *
 * Estende ResponseEntityExceptionHandler, que já reconhece as exceções do Spring MVC (corpo ilegível,
 * rota inexistente, método não permitido, tipo de mídia não suportado, validação...). Sobrescrevemos
 * apenas o formato do corpo e as mensagens.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // ---- Erros da aplicação (lançados pelo service) ----

    @ExceptionHandler(LivroNaoEncontradoException.class)
    public ResponseEntity<Object> tratarNaoEncontrado(LivroNaoEncontradoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), null, request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Object> tratarRegraDeNegocio(RegraDeNegocioException ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of(), null, request);
    }

    // Qualquer falha inesperada: detalhes só no log; o cliente recebe uma mensagem genérica
    // e um requestId para relacionar a resposta à ocorrência no servidor.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Falha na requisição {}", requestId(request), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", List.of(), null, request);
    }

    // ---- Erros do Spring MVC (400, 404, 405, 415...) ----

    // @Valid falhou: 400 com a lista de campos inválidos em "details".
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + " " + erro.getDefaultMessage())
                .sorted()
                .toList();
        return resposta(status, "Falha de validação", details, headers, servletRequest(request));
    }

    // JSON malformado, vazio, com campo desconhecido, tipo errado ou conteúdo extra: 400.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return resposta(status, "JSON inválido, ausente ou incompatível com os campos esperados",
                List.of(), headers, servletRequest(request));
    }

    // Ex.: GET /livros/abc, em que o id deveria ser numérico: 400.
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String nome = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return resposta(status, "Parâmetro inválido: " + nome, List.of(), headers, servletRequest(request));
    }

    // Demais exceções do Spring MVC (404 de rota, 405 com header Allow, 415...) passam por aqui,
    // mantendo o status e os headers que o Spring escolheu, mas com o nosso formato de corpo.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpServletRequest servlet = servletRequest(request);
        if (statusCode.is5xxServerError()) {
            log.error("Falha na requisição {}", requestId(servlet), ex);
        }
        return resposta(statusCode, mensagemPadrao(statusCode), List.of(), headers, servlet);
    }

    private static String mensagemPadrao(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Rota não encontrada";
            case 405 -> "Método não permitido";
            case 406 -> "Formato de resposta não suportado";
            case 415 -> "Use Content-Type: application/json";
            default -> status.is5xxServerError() ? "Erro interno do servidor" : "Requisição inválida";
        };
    }

    // ---- Apoio ----

    private ResponseEntity<Object> resposta(HttpStatusCode status, String mensagem, List<String> detalhes,
            HttpHeaders headers, HttpServletRequest request) {
        var corpo = new ErroResposta(Instant.now().toString(), status.value(), mensagem,
                request.getRequestURI(), detalhes, requestId(request));
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo);
    }

    private static HttpServletRequest servletRequest(WebRequest request) {
        return ((ServletWebRequest) request).getRequest();
    }

    private static String requestId(HttpServletRequest request) {
        return Objects.toString(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE), "");
    }
}
