package br.com.minispring.exemplo.service;

import br.com.minispring.exemplo.exception.LivroNaoEncontradoException;
import br.com.minispring.exemplo.exception.RegraDeNegocioException;
import br.com.minispring.exemplo.model.Livro;
import br.com.minispring.exemplo.model.LivroInput;
import br.com.minispring.exemplo.repository.LivroRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

// @Service (do Spring) substitui o @Service do Mini Spring: o bean é criado e injetado pelo container.
@Service
public class LivroService {
    /** Regra de negócio do tema: um livro não pode passar de 5.000 páginas. */
    public static final int MAX_PAGINAS = 5000;

    private final LivroRepository repository;

    // Injeção por construtor: com um único construtor, o Spring injeta a dependência sozinho (sem @Autowired).
    // Dependemos da interface, permitindo trocar o armazenamento ou usar um fake/mock nos testes.
    public LivroService(LivroRepository repository) {
        this.repository = repository;
    }

    public List<Livro> list(String titulo) {
        // Normalizar os dois lados permite busca sem distinguir maiúsculas de minúsculas.
        // Locale.ROOT evita que a regra varie conforme o idioma configurado na máquina.
        String filter = titulo.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(l -> l.titulo().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    public Livro get(long id) {
        return repository.findById(id).orElseThrow(() -> new LivroNaoEncontradoException(id));
    }

    public Livro create(LivroInput input) {
        // A regra é verificada ANTES de tocar no repositório: se for violada, nada é gravado.
        validateBusinessRule(input);
        return repository.create(input.titulo().trim(), input.numeroPaginas());
    }

    public Livro update(long id, LivroInput input) {
        // Mesma regra do cadastro. Se rejeitar, o livro existente permanece como estava.
        validateBusinessRule(input);
        return repository.update(id, input.titulo().trim(), input.numeroPaginas())
                .orElseThrow(() -> new LivroNaoEncontradoException(id));
    }

    public void delete(long id) {
        if (!repository.delete(id)) throw new LivroNaoEncontradoException(id);
    }

    // Esta regra pertence ao domínio; @NotBlank e @Positive validam apenas a forma da entrada.
    // Chamada direta ao service não passa pela validação HTTP, mas a regra de negócio sempre roda aqui.
    // O service lança exceções de domínio; quem decide o status HTTP (404/422) é o ApiExceptionHandler.
    private void validateBusinessRule(LivroInput input) {
        if (input.numeroPaginas() > MAX_PAGINAS)
            throw new RegraDeNegocioException("Um livro pode ter no máximo " + MAX_PAGINAS + " páginas");
    }
}
