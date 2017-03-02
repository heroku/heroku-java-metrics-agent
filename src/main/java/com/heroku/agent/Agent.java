package com.heroku.agent;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.lang.instrument.Instrumentation;

public class Agent {

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    new Thread() {
      @Override
      public void run() {
        try {
          DefaultExports.initialize();
          Server server = new Server(Integer.valueOf(System.getenv("HEROKU_METRICS_PROM_PORT")));
          ServletContextHandler context = new ServletContextHandler();
          context.setContextPath("/");
          server.setHandler(context);
          context.addServlet(new ServletHolder(new MetricsServlet()), System.getenv("HEROKU_METRICS_PROM_ENDPOINT"));
          server.start();
          server.join();
        } catch (Exception ex) {
          System.out.println("! ERROR: Failed to start metrics servlet");
          ex.printStackTrace();
        }
      }
    }.start();
  }
}

