package edu.college.employee.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;

public final class HttpResponse {
    private final HttpExchange exchange;
    public HttpResponse(HttpExchange exchange) { this.exchange = exchange; }
    public void setStatus(int status) { exchange.setAttribute("status", status); }
    public void setContentType(String value) { exchange.getResponseHeaders().set("Content-Type", value); }
    public void setHeader(String key, String value) { exchange.getResponseHeaders().set(key, value); }
    PrintWriter getWriter() { return new PrintWriter(new OutputStreamWriter(exchange.getResponseBody(), java.nio.charset.StandardCharsets.UTF_8)); }
    HttpExchange exchange() { return exchange; }
}
