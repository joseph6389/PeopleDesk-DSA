package edu.college.employee.web;

import com.sun.net.httpserver.HttpExchange;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public final class HttpRequest {
    private final HttpExchange exchange;
    private LocalSession session;
    public HttpRequest(HttpExchange exchange) { this.exchange = exchange; }
    public String getRequestURI() { return exchange.getRequestURI().getPath(); }
    public String getQuery() { return exchange.getRequestURI().getRawQuery(); }
    public String getMethod() { return exchange.getRequestMethod(); }
    public String getHeader(String name) { return exchange.getRequestHeaders().getFirst(name); }
    JsonNode body() throws IOException { return Web.body(exchange.getRequestBody()); }
    public LocalSession getSession(boolean create) {
        if (session == null) {
            String cookie = getHeader("Cookie"); String id = null;
            if (cookie != null) for (String part : cookie.split(";")) { String p = part.trim(); if (p.startsWith("EMPLOYEE_SESSION=")) id = p.substring(17); }
            session = LocalSession.find(id);
            if (session == null && create) session = LocalSession.create();
        }
        return session;
    }
    public LocalSession getSession() { return getSession(true); }
    public void changeSessionId() { LocalSession s = getSession(true); s.rotate(); }
    HttpExchange exchange() { return exchange; }
    public void writeSessionCookie(HttpResponse response) { LocalSession s=getSession(false); if(s!=null)response.setHeader("Set-Cookie","EMPLOYEE_SESSION="+s.id()+"; Path=/; HttpOnly; SameSite=Lax"); }
}
