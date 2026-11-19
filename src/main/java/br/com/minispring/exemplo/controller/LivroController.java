package br.com.minispring.exemplo.controller;

import br.com.minispring.exemplo.model.Livro;
import br.com.minispring.exemplo.model.LivroInput;
import br.com.minispring.exemplo.service.LivroService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// @RestController + @RequestMapping substituem o @RestController("/livros") do Mini Spring.
// O Spring MVC (DispatcherServlet) faz o roteamento e a conversão JSON <-> objeto.
@RestController
@RequestMapping("/livros")
public class LivroController {
    private final LivroService service;

    // DI por construtor: o container fornece o service. O controller não escolhe
    // como construí-lo, e a dependência fica explícita para quem lê ou testa a classe.
    public LivroController(LivroService service) {
        this.service = service;
    }

    // GET /livros?titulo=java. Sem query param, o default vazio lista todos os livros.
    @GetMapping
    public List<Livro> list(@RequestParam(name = "titulo", defaultValue = "") String titulo) {
        return service.list(titulo);
    }

    // O "id" da anotação corresponde ao {id} da rota. O Spring converte texto em long
    // (se não for número, lança MethodArgumentTypeMismatchException -> 400).
    @GetMapping("/{id}")
    public Livro get(@PathVariable("id") long id) {
        return service.get(id);
    }

    // @Valid dispara a validação do LivroInput (@NotBlank/@Positive) antes de o método começar.
    // 201 + cabeçalho Location apontando para o recurso criado.
    @PostMapping
    public ResponseEntity<Livro> create(@Valid @RequestBody LivroInput input) {
        Livro livro = service.create(input);
        return ResponseEntity.created(URI.create("/livros/" + livro.id())).body(livro);
    }

    // Dados da URL (identidade) + dados do corpo (novos valores). O id da URL é preservado.
    @PutMapping("/{id}")
    public Livro update(@PathVariable("id") long id, @Valid @RequestBody LivroInput input) {
        return service.update(id, input);
    }

    // Não há representação para devolver após excluir: 204 sem corpo.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
