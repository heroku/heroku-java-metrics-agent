package com.heroku.agent.metrics;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.LockSupport;

public class Reporter {
  private final URL endpoint;

  public Reporter(URL endpoint) {
    this.endpoint = endpoint;
  }

  public void report(String jsonString) {
    sendPost(jsonString, Constants.REPORTER_MAX_RETRIES);
  }

  private void sendPost(String jsonString, int remainingRetries) {
    try {
      HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Measurements-Count", "1");
      connection.setRequestProperty("Measurements-Time", iso3339DateTimeStringNow());
      connection.setDoOutput(true);

      DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
      outputStream.writeBytes(jsonString);
      outputStream.flush();
      outputStream.close();

      int responseCode = connection.getResponseCode();

      // HTTP success status codes
      if (responseCode >= 200 && responseCode < 300) {
        return;
      }

      // HTTP client error status codes
      if (responseCode >= 400 && responseCode < 500) {
        try {
          InputStream errorStream = connection.getErrorStream();
          String errorStreamAsString = "<response-without-body>";

          if (errorStream != null) {
            errorStreamAsString = Utils.readAllUtf8(errorStream);
          }

          Logger.logReporterResponseError(
              "send-post", "upstream service error", responseCode, errorStreamAsString);
        } catch (IOException e) {
          Logger.logException("send-post", e);
        }

        return;
      }

      if (remainingRetries <= 0) {
        Logger.logDebug("send-post", "Used up all retries, giving up");
        return;
      }

      LockSupport.parkNanos(Constants.REPORTER_RETRY_DELAY_MS * 1000000);
      sendPost(jsonString, remainingRetries - 1);
    } catch (IOException e) {
      try {
        boolean curlSuccessful = sendPostWithCurl(jsonString);
        if (!curlSuccessful) {
          Logger.logDebug("reporter", "failed to retry with curl (non-zero exit code)");
        }
      } catch (Exception curlException) {
        Logger.logDebug("reporter", "failed to retry with curl (exception)");
      }
    }
  }

  private boolean sendPostWithCurl(String jsonString) throws IOException, InterruptedException {
    Logger.logDebug("reporter", "attempting http post with curl");

    ProcessBuilder processBuilder =
        new ProcessBuilder()
            .command(
                "curl",
                "-H",
                "Content-Type: application/json",
                "-H",
                "Measurements-Count: 1",
                "-H",
                String.format("Measurements-Time: %s", iso3339DateTimeStringNow()),
                "-X",
                "POST",
                "-d",
                jsonString.replace("\n", ""),
                "-L",
                endpoint.toString());

    return processBuilder.start().waitFor() == 0;
  }

  private String iso3339DateTimeStringNow() {
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());
  }
}
