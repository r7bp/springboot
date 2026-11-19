package br.com.minispring.exemplo;

import br.com.minispring.exemplo.controller.LivroController;
import br.com.minispring.exemplo.exception.LivroNaoEncontradoException;
import br.com.minispring.exemplo.exception.RegraDeNegocioException;
import br.com.minispring.exemplo.model.Livro;
import br.com.minispring.exemplo.model.LivroInput;
import br.com.minispring.exemplo.service.LivroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste da camada web: carrega só o Spring MVC (controller, validação, ApiExceptionHandler e filtro),
 * com o LivroService substituído por um mock. Mostra que o Spring faz roteamento, validação e
 * tradução de erros sem precisar do service real nem de um servidor.
 */
@WebMvcTest(LivroController.class)
class LivroControllerTest {
    private static final String VALID_BODY = "{\"titulo\":\"Dom Casmurro\",\"numeroPaginas\":256}";

    @Autowired MockMvc mvc;
    @MockitoBean LivroService service;

    @Test void postCreatesBookWith201AndLocationHeader() throws Exception {
        when(service.create(any(LivroInput.class))).thenReturn(new Livro(1, "Dom Casmurro", 256));

        mvc.perform(post("/livros").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/livros/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Dom Casmurro"));
    }

    @Test void invalidBodyGives400WithDetailsAndNeverReachesService() throws Exception {
        mvc.perform(post("/livros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"  \",\"numeroPaginas\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.length()").value(2));
        verifyNoInteractions(service);
    }

    @Test void putAlsoValidatesBody() throws Exception {
        mvc.perform(put("/livros/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"numeroPaginas\":100}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void getPassesTitleFilterToService() throws Exception {
        when(service.list("java")).thenReturn(List.of(new Livro(1, "Java Efetivo", 416)));

        mvc.perform(get("/livros").param("titulo", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(service).list("java");
    }

    @Test void getWithoutFilterUsesEmptyDefault() throws Exception {
        when(service.list("")).thenReturn(List.of());

        mvc.perform(get("/livros")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(service).list("");
    }

    @Test void missingBookGives404() throws Exception {
        when(service.get(99L)).thenThrow(new LivroNaoEncontradoException(99));

        mvc.perform(get("/livros/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test void businessRuleViolationGives422() throws Exception {
        when(service.create(any(LivroInput.class))).thenThrow(new RegraDeNegocioException("limite de 5000 páginas"));

        mvc.perform(post("/livros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Enorme\",\"numeroPaginas\":5001}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test void nonNumericIdGives400() throws Exception {
        mvc.perform(get("/livros/abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void deleteGives204WithoutBody() throws Exception {
        mvc.perform(delete("/livros/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).delete(1L);
    }

    @Test void unexpectedFailureReturnsGenericJsonWithoutLeakingDetails() throws Exception {
        when(service.list("")).thenThrow(new IllegalStateException("detalhe-interno-do-teste"));

        var result = mvc.perform(get("/livros"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"))
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("detalhe-interno-do-teste"));
        // O mesmo requestId aparece no header e no corpo do erro.
        assertTrue(body.contains(result.getResponse().getHeader("X-Request-Id")));
    }
}
