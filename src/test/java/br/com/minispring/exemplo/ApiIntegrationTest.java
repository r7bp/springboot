package br.com.minispring.exemplo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração: sobe a aplicação Spring Boot completa (Tomcat em porta aleatória) e exercita a API
 * por HTTP real. O contexto é recriado a cada teste para que o repositório em memória comece vazio
 * (o primeiro ID gerado é sempre 1), como acontecia quando o Mini Spring era iniciado a cada teste.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.name=Catálogo de teste")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiIntegrationTest {
    @LocalServerPort private int port;
    private HttpClient client;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void start() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
    @AfterEach void stop() {
        if (client != null) client.close();
    }
    private HttpRequest request(String method, String path, String body, String contentType) {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        if (contentType != null) builder.header("Content-Type", contentType);
        return builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return client.send(request(method, path, body, body == null ? null : "application/json"), HttpResponse.BodyHandlers.ofString());
    }
    private static String livro(String titulo, int paginas) {
        return "{\"titulo\":\"" + titulo + "\",\"numeroPaginas\":" + paginas + "}";
    }
    private JsonNode list(String query) throws Exception {
        return json.readTree(send("GET", "/livros" + query, null).body());
    }

    @Test void fullCrudThroughRealServer() throws Exception {
        assertEquals("Catálogo de teste", json.readTree(send("GET", "/", null).body()).get("application").asText());
        assertEquals(200, send("GET", "/health", null).statusCode());

        var created = send("POST", "/livros", livro(" Dom Casmurro ", 256));
        assertEquals(201, created.statusCode());
        assertTrue(created.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        String location = created.headers().firstValue("Location").orElseThrow();
        assertEquals("/livros/" + json.readTree(created.body()).get("id").asLong(), location);

        var found = send("GET", location, null);
        assertEquals(200, found.statusCode());
        assertEquals("Dom Casmurro", json.readTree(found.body()).get("titulo").asText());

        var updated = send("PUT", location, livro("Dom Casmurro (edição ilustrada)", 300));
        assertEquals(200, updated.statusCode());
        assertEquals(300, json.readTree(updated.body()).get("numeroPaginas").asInt());
        assertEquals(json.readTree(created.body()).get("id").asLong(), json.readTree(updated.body()).get("id").asLong(), "o ID deve ser preservado");

        var removed = send("DELETE", location, null);
        assertEquals(204, removed.statusCode());
        assertEquals("", removed.body());
        assertEquals(404, send("GET", location, null).statusCode());
        assertEquals(404, send("DELETE", location, null).statusCode());
        assertEquals(404, send("PUT", location, livro("Qualquer", 10)).statusCode());
    }

    @Test void listsAndFiltersByPartOfTitleIgnoringCase() throws Exception {
        send("POST", "/livros", livro("Java Efetivo", 400));
        send("POST", "/livros", livro("Introdução ao JAVA", 300));
        send("POST", "/livros", livro("Memórias Póstumas", 250));
        assertEquals(3, list("").size());
        assertEquals(2, list("?titulo=java").size());
        assertEquals(2, list("?titulo=JAVA").size());
        assertEquals(1, list("?titulo=P%C3%93STUMAS").size());
        assertEquals(1, list("?titulo=ntro").size());
        assertEquals(0, list("?titulo=python").size());
    }

    @Test void generatesSequentialIdsAutomatically() throws Exception {
        long first = json.readTree(send("POST", "/livros", livro("Livro A", 1)).body()).get("id").asLong();
        long second = json.readTree(send("POST", "/livros", livro("Livro B", 2)).body()).get("id").asLong();
        assertEquals(first + 1, second);
    }

    @Test void rejectsInvalidInputsWithStructuredErrors() throws Exception {
        var invalid = send("POST", "/livros", livro(" ", 0));
        assertEquals(400, invalid.statusCode());
        var error = json.readTree(invalid.body());
        assertEquals(2, error.get("details").size());
        assertEquals(invalid.headers().firstValue("X-Request-Id").orElseThrow(), error.get("requestId").asText());
        for (String body : List.of("{broken", "null", "", "{}", livro("", 10), livro("Livro", -5),
                "{\"titulo\":\"Livro\",\"numeroPaginas\":1.5}", "{\"titulo\":\"Livro\",\"numeroPaginas\":\"40\"}",
                "{} {}", "{\"titulo\":\"Livro\",\"numeroPaginas\":40,\"id\":7}")) {
            assertEquals(400, send("POST", "/livros", body).statusCode(), body);
        }
        assertEquals(400, send("GET", "/livros/abc", null).statusCode());
        assertEquals(415, client.send(request("POST", "/livros", "{}", "text/plain"), HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(0, list("").size(), "nenhuma entrada inválida pode ter sido gravada");
    }

    @Test void appliesPageLimitOnCreateWith422() throws Exception {
        assertEquals(201, send("POST", "/livros", livro("Limite exato", 5000)).statusCode());
        var rejected = send("POST", "/livros", livro("Passou do limite", 5001));
        assertEquals(422, rejected.statusCode());
        assertTrue(json.readTree(rejected.body()).get("message").asText().contains("5000"));
        assertEquals(1, list("").size(), "o livro rejeitado não pode ser armazenado");
    }

    @Test void appliesSameValidationsOnUpdateAndKeepsDataWhenRejected() throws Exception {
        var created = send("POST", "/livros", livro("Original", 100));
        String location = created.headers().firstValue("Location").orElseThrow();

        assertEquals(422, send("PUT", location, livro("Alterado", 5001)).statusCode());
        assertEquals(400, send("PUT", location, livro(" ", 100)).statusCode());
        assertEquals(400, send("PUT", location, livro("Alterado", 0)).statusCode());
        assertEquals(400, send("PUT", location, "{broken").statusCode());

        var unchanged = json.readTree(send("GET", location, null).body());
        assertEquals("Original", unchanged.get("titulo").asText());
        assertEquals(100, unchanged.get("numeroPaginas").asInt());
        assertEquals(200, send("PUT", location, livro("Limite", 5000)).statusCode());
    }

    @Test void distinguishesRouteErrorsAndSendsAllowHeader() throws Exception {
        var notFound = send("GET", "/inexistente", null);
        assertEquals(404, notFound.statusCode());
        assertEquals(404, json.readTree(notFound.body()).get("status").asInt());

        var response = send("PATCH", "/livros/1", "{}");
        assertEquals(405, response.statusCode());
        String allow = response.headers().firstValue("Allow").orElseThrow();
        for (String method : List.of("GET", "PUT", "DELETE")) assertTrue(allow.contains(method), allow);
        assertFalse(allow.contains("PATCH"), allow);
    }

    @Test void concurrentRequestsProduceUniqueIds() throws Exception {
        var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < 30; i++) futures.add(client.sendAsync(
            request("POST", "/livros", livro("Livro", 10), "application/json"), HttpResponse.BodyHandlers.ofString()));
        var ids = new HashSet<Long>();
        for (var future : futures) {
            var response = future.join();
            assertEquals(201, response.statusCode());
            ids.add(json.readTree(response.body()).get("id").asLong());
        }
        assertEquals(30, ids.size());
        assertEquals(30, list("").size());
    }
}
