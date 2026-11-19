package br.com.minispring.exemplo.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Substitui o LoggingInterceptor do Mini Spring. Todo bean Filter é registrado automaticamente
 * pelo Spring Boot na cadeia de filtros do Tomcat. Gera um requestId por requisição e o expõe no
 * header X-Request-Id, no corpo dos erros (ApiExceptionHandler) e no log do servidor.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // nanoTime mede duração sem depender de ajustes no relógio de calendário.
        long start = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} -> {} ({} ms) requestId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed, requestId);
        }
    }
}
