package com.heroku.agent.metrics;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class Utils {
  /**
   * Fully reads the given {@link InputStream} as a UTF-8 encoded string.
   *
   * @param inputStream The stream to read.
   * @return The data from the given stream, interpreted as a UTF-8 string.
   * @throws IOException If an IO related error occurred.
   */
  public static String readAllUtf8(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();

    byte[] buffer = new byte[1024];

    int lastByteAmountRead;
    while ((lastByteAmountRead = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, lastByteAmountRead);
    }

    return result.toString(StandardCharsets.UTF_8.name());
  }

  private Utils() {}
}
