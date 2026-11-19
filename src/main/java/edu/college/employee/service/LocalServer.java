package edu.college.employee.service;

import com.sun.net.httpserver.*;
import edu.college.employee.web.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public final class LocalServer {
 private final HttpServer server;
 private final ExecutorService executor;
 private final Path assets;
 private LocalServer(int port) throws IOException {
   assets=Path.of(System.getProperty("employee.assets.dir","src/main/webapp")).toAbsolutePath().normalize().toRealPath();
   server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),port),0);
   executor=Executors.newFixedThreadPool(Math.max(4,Runtime.getRuntime().availableProcessors()));
   server.setExecutor(executor);
   server.createContext("/",this::handle);
 }
 private void stop(){server.stop(1);executor.shutdownNow();}
 private void handle(HttpExchange x) throws IOException {
   x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.getResponseHeaders().set("Referrer-Policy","same-origin");
   x.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; style-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
   String path=x.getRequestURI().getPath();String method=x.getRequestMethod();HttpRequest req=new HttpRequest(x);HttpResponse res=new HttpResponse(x);
   try {
     if(path.startsWith("/api/")) {
       boolean open=path.equals("/api/login")||path.equals("/api/register");
       if(!open&&Web.user(req)==null){Web.send(res,401,java.util.Map.of("error","Authentication required"));return;}
       if(Web.user(req)!=null&&!method.equals("GET")&&!method.equals("HEAD")){LocalSession s=req.getSession(false);Object token=s==null?null:s.getAttribute("csrf");if(token==null||!token.equals(req.getHeader("X-CSRF-Token"))){Web.send(res,403,java.util.Map.of("error","Invalid CSRF token"));return;}}
       AuthController auth=new AuthController();ApiController api=new ApiController();
       if(path.equals("/api/register")){if(method.equals("POST"))new RegistrationController().post(req,res);else Web.send(res,405,java.util.Map.of("error","Method not allowed"));}
       else if(path.equals("/api/login")||path.equals("/api/logout")||path.equals("/api/me")){if(method.equals("GET"))auth.get(req,res);else if(method.equals("POST"))auth.post(req,res);else Web.send(res,405,java.util.Map.of("error","Method not allowed"));}
       else if(path.equals("/api/accounts")||path.startsWith("/api/accounts/"))new AccountController().handle(req,res);
       else if(method.equals("GET"))api.get(req,res);else if(method.equals("POST"))api.post(req,res);else if(method.equals("PUT"))api.put(req,res);else if(method.equals("DELETE"))api.delete(req,res);else Web.send(res,405,java.util.Map.of("error","Method not allowed"));
     } else serveAsset(x,path);
   } catch(IllegalArgumentException e){if(!x.getResponseHeaders().containsKey("Content-Type"))Web.error(res,400,e.getMessage()==null?"Invalid request":e.getMessage());}
     catch(Exception e){System.err.println("Request failed: "+e.getClass().getSimpleName());if(!x.getResponseHeaders().containsKey("Content-Type"))Web.error(res,500,"Request could not be completed");}
   finally {x.close();}
 }
 private void serveAsset(HttpExchange x,String path)throws IOException {
   if(!x.getRequestMethod().equals("GET")&&!x.getRequestMethod().equals("HEAD")){x.sendResponseHeaders(405,-1);return;}
   String rel=path.equals("/")?"index.html":path.substring(1);Path file=assets.resolve(rel).normalize();
   if(!file.startsWith(assets)||!Files.isRegularFile(file)){x.sendResponseHeaders(404,-1);return;}
   file=file.toRealPath();if(!file.startsWith(assets)){x.sendResponseHeaders(404,-1);return;}
   String name=file.getFileName().toString().toLowerCase();String type=name.endsWith(".html")?"text/html; charset=utf-8":name.endsWith(".css")?"text/css; charset=utf-8":name.endsWith(".js")?"text/javascript; charset=utf-8":name.endsWith(".svg")?"image/svg+xml":"application/octet-stream";
   x.getResponseHeaders().set("Content-Type",type);x.getResponseHeaders().set("Cache-Control","no-cache");byte[] b=Files.readAllBytes(file);x.sendResponseHeaders(200,x.getRequestMethod().equals("HEAD")?-1:b.length);if(!x.getRequestMethod().equals("HEAD"))x.getResponseBody().write(b);
 }
 public static void main(String[] args)throws Exception {
   int port=Integer.parseInt(System.getenv().getOrDefault("EMPLOYEE_PORT","8080"));if(port<1||port>65535)throw new IllegalArgumentException("EMPLOYEE_PORT must be between 1 and 65535");
    try{LocalServer app=new LocalServer(port);AppData data=App.repo();var employeeTree=App.employeeBinaryTree();Runtime.getRuntime().addShutdownHook(new Thread(app::stop,"peopledesk-shutdown"));app.server.start();System.out.println("PeopleDesk is running at http://127.0.0.1:"+port+"/");System.out.println("Storage: employee records and their binary tree load from and save to data/employee-tree.json.");System.out.println("Loaded employee records: "+employeeTree.size());System.out.println("Loaded employees: "+data.employees().size());System.out.println("Loaded demo accounts: "+data.users().size());new CountDownLatch(1).await();}catch(BindException e){System.err.println("Port "+port+" is already in use. Close the other application or set EMPLOYEE_PORT to a free port.");System.exit(2);}
 }
}


