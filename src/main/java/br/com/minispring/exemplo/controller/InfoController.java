package br.com.minispring.exemplo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {
    private final String appName;

    // @Value injeta configuração (application.properties) na construção do bean, não um parâmetro HTTP.
    // Em Spring a sintaxe é ${chave}; no Mini Spring era apenas "chave".
    public InfoController(@Value("${app.name}") String appName) {
        this.appName = appName;
    }

    @GetMapping("/")
    public Map<String, String> info() {
        return Map.of("application", appName, "framework", "Spring Boot", "server", "Tomcat");
    }

    // Este health check comprova que a aplicação responde; não testa banco ou outros serviços.
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
