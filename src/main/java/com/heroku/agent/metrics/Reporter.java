package com.heroku.agent.metrics;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Joe Kutner on 11/2/17.
 *         Twitter: @codefinger
 */
public class Reporter {

  private URL url;

  private static final Integer MAX_RETRIES = 3;

  public Reporter() throws MalformedURLException {
    this(System.getenv("HEROKU_METRICS_URL"));
  }

  public Reporter(String urlString) throws MalformedURLException {
    this(urlString == null? null : new URL(urlString));
  }

  public Reporter(URL url) throws MalformedURLException {
    this.url = url;
  }

  public Boolean enabled() {
    return this.url != null;
  }

  public Boolean report(String message) throws IOException {
    if (enabled()) {
      return sendPost(message);
    } else {
      return false;
    }
  }

  private Boolean sendPost(String message) throws IOException {
    return sendPost(message, 0);
  }

  private Boolean sendPost(String message, Integer retries) throws IOException {
    try {
      HttpURLConnection con = (HttpURLConnection) url.openConnection();

      con.setRequestMethod("POST");

      String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());

      con.setRequestProperty("Content-Type", "application/json");
      con.setRequestProperty("Measurements-Count", "1");
      con.setRequestProperty("Measurements-Time", now);

      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());

      wr.writeBytes(message);
      wr.flush();
      wr.close();

      int responseCode = con.getResponseCode();

      if (responseCode >= 400 && responseCode < 500) {
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        System.out.println("error at=send-post component=heroku-java-metrics-agent message=\"upstream service error\" status=" + responseCode + " response=\"" + response + "\"");
        return false;
      } else if (responseCode >= 200 && responseCode < 300) {
        return true;
      } else {
        if (retries > MAX_RETRIES) {
          return false;
        } else {
          LockSupport.parkNanos(2 * 1000000);
          return sendPost(message, retries + 1);
        }
      }
    } catch (Exception e) {
      try {
        if (curl(url.toString(), message)) {
          return true;
        } else {
          throw new IOException("failed to retry with curl", e);
        }
      } catch (Exception curlException) {
        if (e instanceof IOException) {
          throw e;
        } else {
          throw new IOException("failed to retry with curl", e);
        }
      }
    }
  }

  private Boolean curl(String urlStr, String message) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder().command("curl", "-X", "POST", "-d", message, "-L", urlStr);
    return pb.start().waitFor() == 0;
  }
}
