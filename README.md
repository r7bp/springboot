# API de Catálogo de Livros — Grupo 6

API REST em **Java 21** para cadastro dos livros de uma biblioteca, **migrada do framework didático Mini Spring (projeto-base: <https://github.com/ferreiralisson/mini-spring>) para Spring Boot 3.5**. O tema, as rotas, os filtros e as regras de negócio foram mantidos; o que mudou foi quem assume as responsabilidades de infraestrutura (servidor, IoC, roteamento, JSON, validação e erros): agora é o Spring. Os dados continuam em memória.

## Integrantes

| Nome | Matrícula | Usuário no GitHub |
|------|-----------|-------------------|
| Caique Lucas Ribeiro Lupercinio dos Santos | 202517640070 | caique-lucas |
| Davyd de Oliveira Melo | 202517640072 | EasyDavyd |
| Gleysson Robert de Oliveira Silva | 202517640014 | r7bp |
| Pablo Henrique Braz dos Santos | 202517640046 | PabloHenrique0707 |
| Ruan Henry Nascimento Cunha | 202517640037 | ruanhenry10 |
| Victor Kauã Dias Ferreira | 202517640039 | Maquista323 |
| Jefferson Vitor da Silva Lima | 202517640029 | JeffinzinLima |

## Uso de IA

Foi utilizada a IA **Claude (Anthropic)** como apoio na adaptação do projeto-base para o domínio de livros, na **migração para Spring Boot**, na escrita dos testes, do `requests.http`, da coleção Postman e deste README. Todo o código foi lido, executado e é compreendido por todos os integrantes do grupo, que são responsáveis por explicá-lo.

## Versão anterior (Mini Spring)

A versão original, com o Mini Spring, está preservada para comparação:

- branch **`mini-spring`**;
- tag **`v1.0-mini-spring`** (mesmo commit).

Para ver tudo o que mudou: `git diff v1.0-mini-spring..main --stat` (ou pela página *Compare* do GitHub).

## O que mudou na migração

| Responsabilidade | Antes (Mini Spring) | Agora (Spring Boot) |
| --- | --- | --- |
| Iniciar a aplicação e o servidor | `MiniApplication.run` + Jetty montado à mão | `SpringApplication.run` + **Tomcat embutido** (`@SpringBootApplication`) |
| Descobrir e criar os beans (IoC) | `ComponentScanner` + `ApplicationContext` próprios | `@ComponentScan` (dentro de `@SpringBootApplication`) + `ApplicationContext` do Spring |
| Estereótipos | `@RestController`, `@Service`, `@Repository` do pacote `framework` | `@RestController`, `@Service`, `@Repository` do Spring |
| Injeção de dependências | Construtor, resolvida pelo `ApplicationContext` | **Continua por construtor**; o Spring injeta sozinho (um único construtor dispensa `@Autowired`) |
| Roteamento | `Router` + `@Route(method, path)` | `DispatcherServlet` do Spring MVC + `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` |
| Parâmetros HTTP | `DispatcherServlet.bind` + `@PathVariable`, `@RequestParam`, `@RequestBody` próprios | Mesmas anotações, agora do Spring (`HandlerMethodArgumentResolver`s internos) |
| JSON ↔ objeto | `ObjectMapper` criado no dispatcher | Jackson configurado pelo Spring Boot (opções estritas em `application.properties`) |
| Respostas | `HttpResult` | `ResponseEntity` (`created(...)`, `noContent()`); retorno comum vira `200` |
| Validação | `@NotBlank`/`@Positive` próprios + `Validator` | **Bean Validation** (`jakarta.validation`) + `@Valid` no controller |
| Tratamento de erros | `HttpException` + `catch` no `DispatcherServlet`; o service conhecia HTTP | Exceções de domínio (`LivroNaoEncontradoException`, `RegraDeNegocioException`) + `@RestControllerAdvice` (`ApiExceptionHandler`) |
| `requestId`, cronômetro e log | `LoggingInterceptor` | `RequestIdFilter` (`OncePerRequestFilter`) |
| Configuração | `AppConfig` + `@Value("app.name")` | `application.properties` + `@Value("${app.name}")` |
| Build | `maven-shade-plugin` | `spring-boot-maven-plugin` + `spring-boot-starter-parent` |

**O que foi preservado:** o pacote `exemplo` com as camadas `controller` / `service` / `repository` / `model`, a interface `LivroRepository` e a implementação em memória, o CRUD, o filtro por título, o limite de 5.000 páginas, os status HTTP e o formato de erro (`timestamp`, `status`, `message`, `path`, `details`, `requestId`). O pacote `framework` foi **removido**.

**Diferenças de comportamento (consequência de trocar o servidor):** `GET /` agora informa `Spring Boot` e `Tomcat`; o cabeçalho `Allow` do `405` lista os métodos na ordem escolhida pelo Spring; o limite de 1 MiB para o corpo da requisição (que era do Mini Spring) não existe mais.

## Domínio

Uma biblioteca precisa manter o catálogo de seus livros. A entidade principal é o **Livro**, com:

| Campo | Tipo | Observação |
| --- | --- | --- |
| `id` | número | Gerado automaticamente; nunca informado pelo cliente; preservado nas atualizações |
| `titulo` | texto | Obrigatório; não aceita vazio nem apenas espaços; espaços nas pontas são removidos |
| `numeroPaginas` | inteiro | Maior que zero e no máximo **5.000** |

Escopo: apenas o catálogo (sem empréstimos, autores ou usuários).

## Regra de negócio

> Um livro pode ter, no máximo, **5.000 páginas**.

A regra é aplicada na camada **Service**, no cadastro e na atualização. Se violada, o service lança `RegraDeNegocioException`, que o `ApiExceptionHandler` converte em **422**, e **nada é gravado**: a verificação acontece antes de qualquer chamada ao repositório, então um `PUT` rejeitado deixa o livro existente exatamente como estava.

As validações estruturais (`@NotBlank` em `titulo`, `@Positive` em `numeroPaginas`) ficam no DTO `LivroInput` e são aplicadas pelo Spring antes do método do controller (`@Valid`), tanto no `POST` quanto no `PUT`. Elas geram **400**.

## Como executar

Pré-requisitos: **JDK 21** e **Maven 3.9+** (`java -version` e `mvn -version`). Na primeira compilação é preciso internet para baixar as dependências.

```bash
git clone <URL-DO-REPOSITORIO>
cd <pasta-do-repositorio>

mvn clean verify                          # compila, roda os testes e gera o JAR
java -jar target/livros-api-2.0.0.jar     # sobe a API em http://127.0.0.1:8080
```

Alternativa durante o desenvolvimento: `mvn spring-boot:run`. Pela IDE, execute a classe `br.com.minispring.exemplo.Application`.

Confirme que está no ar: `curl -i http://127.0.0.1:8080/health` → `200` e `{"status":"UP"}`.

Para trocar a porta: `java -jar target/livros-api-2.0.0.jar --server.port=9090`. Executar só os testes: `mvn test`.

## Rotas

Base: `http://127.0.0.1:8080` · corpo e respostas em JSON.

| Método | Rota | Descrição | Sucesso |
| --- | --- | --- | --- |
| `POST` | `/livros` | Cadastra um livro | `201` + cabeçalho `Location: /livros/{id}` |
| `GET` | `/livros` | Lista todos | `200` |
| `GET` | `/livros?titulo=java` | Filtra por parte do título, sem diferenciar maiúsculas/minúsculas | `200` |
| `GET` | `/livros/{id}` | Consulta por ID | `200` |
| `PUT` | `/livros/{id}` | Atualiza (o ID é preservado) | `200` |
| `DELETE` | `/livros/{id}` | Exclui | `204` (sem corpo) |
| `GET` | `/` e `/health` | Identificação e saúde da aplicação | `200` |

Corpo de `POST` e `PUT`:

```json
{ "titulo": "Dom Casmurro", "numeroPaginas": 256 }
```

### Respostas de erro

| Status | Quando |
| --- | --- |
| `400` | Título vazio/em branco, páginas ≤ 0, JSON inválido ou ausente, campo desconhecido (ex.: `id`), tipo errado, ID não numérico |
| `404` | Livro inexistente (GET, PUT ou DELETE) ou rota inexistente |
| `422` | Regra de negócio violada: mais de 5.000 páginas |
| `405` / `415` | Método não permitido / `Content-Type` diferente de `application/json` |
| `500` | Falha inesperada: mensagem genérica; os detalhes ficam só no log, ligados pelo `requestId` |

Erros têm formato uniforme: `timestamp`, `status`, `message`, `path`, `details` e `requestId` (o mesmo valor do cabeçalho `X-Request-Id`).

## Testando

- **`requests.http`**: exemplos de sucesso e de erro para o cliente HTTP do IntelliJ ou a extensão REST Client do VS Code. Rode em ordem, com a aplicação recém-iniciada.
- **Postman**: importe `postman/Livros-API.postman_collection.json` e `postman/Livros-API.local.postman_environment.json` (*Import* → arquivos) e selecione o ambiente **Livros API - Local**. A coleção tem 28 requisições em três pastas (Sucesso, Erros, Limpeza), com testes automáticos de status e salva o ID criado na variável `livroId`. Para rodar tudo de uma vez, use *Run collection*.
- **Testes automatizados** (`mvn test`):
  - `LivroServiceTest`: unitário, regra de negócio na camada Service, sem Spring.
  - `LivroControllerTest`: camada web com `@WebMvcTest` e `MockMvc`; o service é um mock. Verifica rotas, `@Valid`, status e formato de erro.
  - `ApiIntegrationTest`: `@SpringBootTest` com Tomcat real em porta aleatória; exercita CRUD, filtro, IDs, 400/404/405/415/422, limites 5000/5001, concorrência e garante que operações rejeitadas não alteram os dados.

## Arquitetura

```
src/main/java/br/com/minispring/exemplo/
├── Application.java       @SpringBootApplication: ponto de entrada
├── controller/            LivroController, InfoController — contrato HTTP, sem regra de negócio
├── service/               LivroService — regra de negócio (limite de 5.000 páginas)
├── repository/            LivroRepository (interface) + InMemoryLivroRepository
├── model/                 Livro (entidade) e LivroInput (DTO de entrada com validação)
├── exception/             LivroNaoEncontradoException, RegraDeNegocioException
└── web/                   ApiExceptionHandler (@RestControllerAdvice), RequestIdFilter, ErroResposta
```

| Camada | Responsabilidade |
| --- | --- |
| **Controller** | Receber a requisição já convertida/validada pelo Spring e devolver `ResponseEntity`/objeto. Não decide regras. |
| **Service** | Aplicar a regra de negócio, normalizar o título, lançar exceção de domínio quando o livro não existe. |
| **Repository** | Guardar e buscar livros em memória e gerar IDs. Não conhece HTTP nem regras. |
| **ApiExceptionHandler** | Traduzir exceções em respostas HTTP (400, 404, 405, 415, 422, 500) no formato uniforme. |

**Injeção de dependências por construtor:** `LivroController(LivroService)` → `LivroService(LivroRepository)` → `InMemoryLivroRepository`. O service depende da *interface* `LivroRepository`; o container do Spring descobre a implementação anotada com `@Repository` e a entrega na inicialização.

### Caminho de uma requisição (`POST /livros`)

1. O **Tomcat** embutido recebe o HTTP e passa pela cadeia de filtros; o `RequestIdFilter` gera o `requestId` e o cabeçalho `X-Request-Id`.
2. O **DispatcherServlet** do Spring MVC encontra `LivroController.create` pelo verbo + caminho (`@PostMapping` em `/livros`).
3. O **Jackson** converte o JSON em `LivroInput` e o **Bean Validation** aplica `@NotBlank`/`@Positive` por causa do `@Valid` (falha → `MethodArgumentNotValidException` → 400 pelo `ApiExceptionHandler`).
4. O **Controller** chama `LivroService.create(input)`.
5. O **Service** verifica o limite de 5.000 páginas (falha → `RegraDeNegocioException` → 422) e só então chama o **Repository**.
6. O **Repository** gera o ID, guarda o `Livro` em memória e o devolve.
7. O Controller responde `ResponseEntity.created("/livros/{id}", livro)` → o Spring serializa em JSON e envia **201** com `Location`.

## Limitações

Dados em memória (perdidos ao reiniciar), sem banco de dados, frontend ou autenticação, conforme o escopo da atividade.
