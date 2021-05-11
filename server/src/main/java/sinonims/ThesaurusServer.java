package sinonims;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ThesaurusServer {

  static ThesaurusConfig conf = null;
  static Dictionary dict = null;

  public static void main(String[] args) throws Exception {

    if (args.length != 2 || !args[0].equals("--config")) {
      System.out.println("Usage: " + ThesaurusServer.class.getSimpleName() + " --config propertyFile");
      System.exit(1);
    }
    conf = new ThesaurusConfig(args);
    dict = new Dictionary(conf);

//    Response response = dict.getResponse("avia");
//    Gson gson = new Gson();
//    String jsonResponse = gson.toJson(response);

    // log(dict.printAllDict());
    /*
     * for (String searchedWord : searchedWords) { System.out.print("\n\n** CERCA: "
     * + searchedWord); System.out.print(dict.printWord(searchedWord)); }
     */

    HttpServer server = HttpServer.create(new InetSocketAddress(conf.serverPort), 0);
    log("Server enabled on port: " + conf.serverPort + "; path: " + conf.urlPath);
    log("Synonims server enabled on port: " + conf.serverPort + "; path: " + conf.urlPath);
    server.createContext(conf.urlPath, new MyHandler());
    server.setExecutor(null); // creates a default executor
    server.start();

  }

  static class MyHandler implements HttpHandler {

    public void handle(HttpExchange t) throws IOException {

      GsonBuilder builder = new GsonBuilder();
      builder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.PRIVATE);
      Gson gson = builder.create();

      String url = t.getRequestURI().toString().substring(conf.urlPath.length());
      String apiCall = "";
      try {
        apiCall = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        // not going to happen - value came from JDK's own StandardCharsets
      }
      apiCall = apiCall.trim();
      log("API call: " + apiCall);
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

  private static void log(String comment) {
    if (conf.logging.equals("on") && !comment.isEmpty()) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z ");
      Date date = new Date(System.currentTimeMillis());
      System.out.println(formatter.format(date) + comment);
    }
  }
}
