package br.com.minispring.exemplo.web;

import java.util.List;

/** Formato uniforme de erro devolvido pela API (o mesmo que o Mini Spring já usava). */
public record ErroResposta(String timestamp, int status, String message, String path,
                           List<String> details, String requestId) {}
