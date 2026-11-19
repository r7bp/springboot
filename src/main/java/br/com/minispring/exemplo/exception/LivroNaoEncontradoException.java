package br.com.minispring.exemplo.exception;

/** Livro inexistente. O ApiExceptionHandler a traduz para HTTP 404; o service não conhece HTTP. */
public class LivroNaoEncontradoException extends RuntimeException {
    public LivroNaoEncontradoException(long id) {
        super("Livro não encontrado: " + id);
    }
}
