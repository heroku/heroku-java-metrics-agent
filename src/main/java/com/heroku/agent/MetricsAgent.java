package com.heroku.agent;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsAgent {

  private static final String classpathPattern = "cp=(.*)(\\:?)(,?)";

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    if (System.getenv("HEROKU_METRICS_PROM_PORT") == null) {
      System.out.println("! ERROR: Could not find HEROKU_METRICS_PROM_PORT.");
      return;
    }
    if (System.getenv("HEROKU_METRICS_PROM_ENDPOINT") == null) {
      System.out.println("! ERROR: Could not find HEROKU_METRICS_PROM_ENDPOINT.");
      return;
    }

    if (agentArgs != null && agentArgs.contains("cp=")) {
      Pattern r = Pattern.compile(classpathPattern);
      Matcher m = r.matcher(agentArgs);
      if (m.find()) {
        loadExtClasspath(m.group(1));
      }
    }

    try {
      DefaultExports.initialize();
      Server server = new Server(Integer.valueOf(System.getenv("HEROKU_METRICS_PROM_PORT")));
      ServletContextHandler context = new ServletContextHandler();
      context.setContextPath("/");
      server.setHandler(context);
      context.addServlet(new ServletHolder(new MetricsServlet()), System.getenv("HEROKU_METRICS_PROM_ENDPOINT"));
      server.start();
    } catch (Exception ex) {
      System.out.println("! ERROR: Failed to start metrics servlet");
      ex.printStackTrace();
    }
  }

  private static void loadExtClasspath(String file) {
    final Class[] parameters = new Class[]{URL.class};
    try {
      URL u = new URL("file://" + file);
      URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      Class sysclass = URLClassLoader.class;
      Method method = sysclass.getDeclaredMethod("addURL", parameters);
      method.setAccessible(true);
      method.invoke(sysloader, new Object[]{u});
    } catch (Throwable t) {
      System.out.println("! ERROR: Failed to load servlet-api JAR.");
      t.printStackTrace();
    }
  }
}
