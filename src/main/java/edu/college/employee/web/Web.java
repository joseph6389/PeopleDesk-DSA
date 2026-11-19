package edu.college.employee.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.util.Map;

public final class Web {
 public static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 public static void send(HttpResponse r,int status,Object body)throws IOException { r.setStatus(status); r.setContentType("application/json;charset=UTF-8"); r.setHeader("Cache-Control","no-store"); byte[] bytes=JSON.writeValueAsBytes(body); r.exchange().sendResponseHeaders(status,bytes.length); r.exchange().getResponseBody().write(bytes); }
 public static JsonNode body(InputStream in)throws IOException { byte[] data=in.readNBytes(1_048_577); if(data.length>1_048_576)throw new IllegalArgumentException("Request is too large"); JsonNode n; try { n=JSON.readTree(data); } catch (JsonProcessingException e) { throw new IllegalArgumentException("Malformed JSON"); } if(n==null||!n.isObject())throw new IllegalArgumentException("JSON object required");return n; }
 public static JsonNode body(HttpRequest r)throws IOException { return r.body(); }
 public static String str(JsonNode n,String key){JsonNode v=n.get(key);if(v==null||!v.isTextual())throw new IllegalArgumentException("Invalid "+key);return v.asText();}
 public static UserSession user(HttpRequest r){LocalSession s=r.getSession(false);Object o=s==null?null:s.getAttribute("user");if(!(o instanceof UserSession user))return null;return edu.college.employee.service.App.repo().users().stream().anyMatch(account->account.username().equalsIgnoreCase(user.username())&&account.active()&&account.role().equals(user.role()))?user:null;}
 public static void error(HttpResponse r,int status,String message){try{send(r,status,Map.of("error",message));}catch(IOException ignored){}}
 public record UserSession(String username,String role,String employeeId){}
}
