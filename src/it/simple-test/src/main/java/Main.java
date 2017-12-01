import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

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
      int ch;
      StringBuilder sb = new StringBuilder();
      while((ch = t.getRequestBody().read()) != -1) {
        sb.append((char) ch);
      }

      System.out.println(sb.toString());

      System.out.println(t.getRequestBody());
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