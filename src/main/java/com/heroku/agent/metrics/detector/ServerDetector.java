package com.heroku.agent.metrics.detector;

import java.lang.instrument.Instrumentation;

public interface ServerDetector {

  boolean detect();

  /**
   * Notify detector that the JVM is about to start. A detector can, if needed, block and wait for some condition but
   * should ultimately return at some point or throw an exception. This notification is executed
   * in a very early stage (premain of the JVM agent) before the main class of the Server is executed.
   * @param instrumentation the Instrumentation implementation
   */
  void jvmAgentStartup(Instrumentation instrumentation);
}
