package org.softcatala.sinonims;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.tools.LtThreadPoolFactory;

public class ThesaurusServer {

    static ThesaurusConfig conf = null;
    static Dictionary dict = null;
    
    private static ThreadPoolExecutor executorService;

    protected static Logger logger;

    public static void main(String[] args) throws Exception {

        if (logger == null) {
            logger = getLogger();
        }

        if (args.length != 2 || !args[0].equals("--config")) {
            logger.error("Usage: " + ThesaurusServer.class.getSimpleName() + " --config propertyFile");
            System.exit(1);
        }

        conf = new ThesaurusConfig(args);
        dict = new Dictionary(conf);
        
//    Response response = dict.getResponse("providència");
//    Gson gson = new Gson();
//    String jsonResponse = gson.toJson(response);

        // log(dict.printAllDict());
        /*
         * for (String searchedWord : searchedWords) { System.out.print("\n\n** CERCA: "
         * + searchedWord); System.out.print(dict.printWord(searchedWord)); }
         */

        logger.warn(conf);
        if (conf.production.equalsIgnoreCase("no")) {
          logger.warn("Exiting");
          System.exit(0);
        }
        
        executorService = getExecutorService(conf);
        HttpServer server = HttpServer.create(new InetSocketAddress(conf.serverPort), 0);
        //server.setExecutor(null); // creates a default executor
        server.setExecutor(executorService);
        server.createContext(conf.urlPath, new MyHandler());
        server.start();
        logger.warn("Server enabled on port: " + conf.serverPort + "; path: " + conf.urlPath);
        logger.warn("Synonyms server enabled on port: " + conf.serverPort + "; path: " + conf.urlPath);
    }

    static class MyHandler implements HttpHandler {

        public void handle(HttpExchange t) throws IOException {

            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.PRIVATE);
            Gson gson = builder.create();

            String userAgent = t.getRequestHeaders().get("user-agent").toString();
            
            String url = t.getRequestURI().toString().substring(conf.urlPath.length());
            String apiCall = "";
            try {
                apiCall = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // not going to happen - value came from JDK's own StandardCharsets
            }
            apiCall = apiCall.trim();
            log("API call: " + apiCall + " UserAgent: " + userAgent);
            String[] parts = apiCall.split("/");
            int code = 200;
            if (parts.length == 2) {
                String jsonResponse = null;
                if (parts[0].equalsIgnoreCase("search")) {
                    Response response = dict.getResponse(parts[1]);
                    jsonResponse = gson.toJson(response);
                    if (response.results == null || response.results.size() == 0) {
                        code = 404;
                    }
                } else if (parts[0].equalsIgnoreCase("index")) {
                    Index index = dict.getIndex(parts[1]);
                    jsonResponse = gson.toJson(index);
                    if (index.words == null || index.words.size() == 0) {
                        code = 404;
                    }
                } else if (parts[0].equalsIgnoreCase("autocomplete")) {
                    Index index = dict.getAutocomplete(parts[1]);
                    jsonResponse = gson.toJson(index);
                    if (index.words == null || index.words.size() == 0) {
                        code = 404;
                    }
                }
                if (jsonResponse != null) {
                    t.getResponseHeaders().add("Content-type", "application/json");
                    t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    t.sendResponseHeaders(code, jsonResponse.getBytes().length);
                    OutputStream os = t.getResponseBody();
                    os.write(jsonResponse.getBytes());
                    os.close();
                    return;
                }
            }
            // Error 500
            t.sendResponseHeaders(500, -1);
        }
    }

    static void log(String comment) {
        if (conf.logging.equals("on") && !comment.isEmpty()) {
            System.out.println(comment);
            logger.info(comment);
        }
    }

    private static Logger getLogger() {
        return LogManager.getLogger();
    }
    
    private static ThreadPoolExecutor getExecutorService(ThesaurusConfig conf) {
      int threadPoolSize = conf.maxCheckThreads;
      log("Setting up thread pool with " + threadPoolSize + " threads");
      return LtThreadPoolFactory.createFixedThreadPoolExecutor(LtThreadPoolFactory.SERVER_POOL,
        threadPoolSize, threadPoolSize, 0,0L, false,
        (thread, throwable) -> log("Thread: " + thread.getName() + " failed with: " + throwable.getMessage()), false);
    }
}
