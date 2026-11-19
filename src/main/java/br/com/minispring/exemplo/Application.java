package br.com.minispring.exemplo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada. @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * O Spring varre este pacote e os subpacotes (controller, service, repository...) e cria os beans;
 * isso substitui o MiniApplication + ComponentScanner + ApplicationContext do Mini Spring.
 * O Tomcat embutido é iniciado automaticamente na porta de server.port.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
