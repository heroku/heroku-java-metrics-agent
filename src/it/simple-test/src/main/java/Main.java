import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.json.*;

public class Main {

  public static void main(String[] args) throws Exception {
    Executors.newSingleThreadExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          System.out.println(System.getenv("HEROKU_METRICS_URL"));
          int port = 9876;
          HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
          server.createContext("/", new MyHandler());
          server.setExecutor(null); // creates a default executor
          server.start();
        } catch (IOException e) {
          System.out.println("server: error starting server");
          e.printStackTrace();
          System.exit(1);
        }
      }
    });

    Thread.sleep(10000);
    System.out.println("server: timed-out waiting for metrics");
    System.exit(1);
  }

  static class MyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      if (!"POST".equals(t.getRequestMethod())) {
        System.out.println("server: unexpected HTTP method (" + t.getRequestMethod() + ")");
        System.exit(1);
      }

      int ch;
      StringBuilder sb = new StringBuilder();
      while((ch = t.getRequestBody().read()) != -1) {
        sb.append((char) ch);
      }

      System.out.println(sb.toString());

      JSONObject obj = new JSONObject(sb.toString());

      try {
        System.out.println(obj.getJSONObject("gauges").get("jvm_memory_bytes_used.area_heap"));
        System.out.println(obj.getJSONObject("gauges").get("jvm_buffer_pool_bytes_capacity.name_direct"));
        System.out.println(obj.getJSONObject("counters").get("jvm_buffer_pool_count.name_direct"));
        System.out.println(obj.getJSONObject("counters").get("jvm_gc_collection_seconds_count.gc_all"));
      } catch (Exception e) {
        System.out.println("server: error parsing json");
        e.printStackTrace();
        System.exit(1);
      }

      String response = "OK";
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();

      System.out.println("server: received metrics");
      System.exit(0);
    }
  }
}