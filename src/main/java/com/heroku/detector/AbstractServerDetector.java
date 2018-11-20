package com.heroku.detector;

import java.lang.instrument.Instrumentation;

public abstract class AbstractServerDetector implements ServerDetector {

  /**
   * By default do nothing
   */
  public boolean detect() {
    return false;
  }

  /**
   * By default do nothing during JVM agent startup
   */
  public void jvmAgentStartup(Instrumentation instrumentation) {
  }

  /**
   * Tests if the given class name has been loaded by the JVM. Don't use this method
   * in case you have access to the class loader which will be loading the class
   * because the used approach is not very efficient.
   * @param className the name of the class to check
   * @param instrumentation the Instrumentation implementation
   * @return true if the class has been loaded by the JVM
   * @throws IllegalArgumentException in case instrumentation or the provided class is null
   */
  boolean isClassLoaded(String className, Instrumentation instrumentation) {
    if (instrumentation == null || className == null) {
      throw new IllegalArgumentException("instrumentation and className must not be null");
    }
    Class<?>[] classes = instrumentation.getAllLoadedClasses();
    for (Class<?> c : classes) {
      if (className.equals(c.getName())) {
        return true;
      }
    }
    return false;
  }

}
