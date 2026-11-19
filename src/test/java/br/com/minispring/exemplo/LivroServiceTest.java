package br.com.minispring.exemplo;

import br.com.minispring.exemplo.exception.LivroNaoEncontradoException;
import br.com.minispring.exemplo.exception.RegraDeNegocioException;
import br.com.minispring.exemplo.model.Livro;
import br.com.minispring.exemplo.model.LivroInput;
import br.com.minispring.exemplo.repository.InMemoryLivroRepository;
import br.com.minispring.exemplo.service.LivroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Teste unitário da camada Service, sem Spring: o repositório é injetado pelo construtor. */
class LivroServiceTest {
    private InMemoryLivroRepository repository;
    private LivroService service;

    @BeforeEach void setUp() {
        repository = new InMemoryLivroRepository();
        service = new LivroService(repository);
    }

    @Test void createsTrimmedTitleAndGeneratedId() {
        Livro livro = service.create(new LivroInput("  O Cortiço  ", 200));
        assertEquals(1, livro.id());
        assertEquals("O Cortiço", livro.titulo());
    }

    @Test void acceptsExactLimitAndRejectsAboveWithBusinessRuleException() {
        assertEquals(5000, service.create(new LivroInput("Enorme", 5000)).numeroPaginas());
        var e = assertThrows(RegraDeNegocioException.class, () -> service.create(new LivroInput("Maior", 5001)));
        assertTrue(e.getMessage().contains("5000"));
        assertEquals(1, repository.findAll().size());
    }

    @Test void rejectedUpdateDoesNotChangeStoredBook() {
        Livro original = service.create(new LivroInput("Original", 100));
        assertThrows(RegraDeNegocioException.class, () -> service.update(original.id(), new LivroInput("Novo", 6000)));
        assertEquals(original, service.get(original.id()));
    }

    @Test void updatePreservesId() {
        Livro original = service.create(new LivroInput("Original", 100));
        Livro atualizado = service.update(original.id(), new LivroInput("Atualizado", 150));
        assertEquals(original.id(), atualizado.id());
        assertEquals("Atualizado", service.get(original.id()).titulo());
    }

    @Test void missingBookThrowsNotFoundForGetUpdateAndDelete() {
        assertThrows(LivroNaoEncontradoException.class, () -> service.get(99));
        assertThrows(LivroNaoEncontradoException.class, () -> service.update(99, new LivroInput("X", 1)));
        assertThrows(LivroNaoEncontradoException.class, () -> service.delete(99));
    }

    @Test void deleteRemovesTheBook() {
        Livro livro = service.create(new LivroInput("Temporário", 10));
        service.delete(livro.id());
        assertThrows(LivroNaoEncontradoException.class, () -> service.get(livro.id()));
    }

    @Test void filterIgnoresCaseAndMatchesPartOfTitle() {
        service.create(new LivroInput("Clean Code", 464));
        service.create(new LivroInput("Código Limpo", 425));
        assertEquals(1, service.list("CLEAN").size());
        assertEquals(1, service.list("limpo").size());
        assertEquals(2, service.list("").size());
    }
}
