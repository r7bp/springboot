package br.com.minispring.exemplo.exception;

/** Regra de negócio violada (ex.: mais de 5.000 páginas). O ApiExceptionHandler a traduz para HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String message) {
        super(message);
    }
}
