package com.heroku.detector;

import java.lang.instrument.Instrumentation;

public class JBossDetector extends AbstractServerDetector {

  @Override
  public boolean detect() {
    return earlyDetectForJBossModulesBasedContainer(this.getClass().getClassLoader());
  }

  /**
   * Attempts to detect a JBoss modules based application server. Because getting
   * access to the main arguments is not possible, it returns true in case the system property
   * {@code jboss.modules.system.pkgs} is set and the {@code org/jboss/modules/Main.class} resource can be found
   * using the class loader of this class.
   *
   * If so, it awaits the early initialization of a JBoss modules based application server by polling the system property
   * {@code java.util.logging.manager} and waiting until the specified class specified by this property has been
   * loaded by the JVM.
   */
  @Override
  public void jvmAgentStartup(Instrumentation instrumentation) {
    jvmAgentStartup(instrumentation, this.getClass().getClassLoader());
  }

  protected void jvmAgentStartup(Instrumentation instrumentation, ClassLoader classLoader) {
    if (earlyDetectForJBossModulesBasedContainer(classLoader)) {
      awaitServerInitializationForJBossModulesBasedContainer(instrumentation);
    }
  }

  private boolean earlyDetectForJBossModulesBasedContainer(ClassLoader classLoader) {
    return hasWildflyProperties() &&
        // Contained in any JBoss modules app:
        classLoader.getResource("org/jboss/modules/Main.class") != null;
  }

  private boolean hasWildflyProperties() {
    // For Wildfly AS:
    if (System.getProperty("jboss.modules.system.pkgs") != null) {
      return true;
    }
    // For Thorntail (Wildfly Swarm):
    String bootModuleLoader = System.getProperty("boot.module.loader");
    if (bootModuleLoader != null) {
      System.out.println("bootModuleLoader: " + bootModuleLoader);
      return bootModuleLoader.contains("wildfly");
    }
    // For Thorntail (Wildfly Swarm):
    String swarmPort = System.getProperty("swarm.http.port");
    if (swarmPort != null) {
      System.out.println("swarmPort: " + swarmPort);
      return true;
    }
    return false;
  }

  // Wait a max 5 Minutes
  private static final int LOGGING_DETECT_TIMEOUT = 5 * 60 * 1000;
  private static final int LOGGING_DETECT_INTERVAL = 200;

  private void awaitServerInitializationForJBossModulesBasedContainer(Instrumentation instrumentation) {
    int count = 0;
    while (count * LOGGING_DETECT_INTERVAL < LOGGING_DETECT_TIMEOUT) {
      String loggingManagerClassName = System.getProperty("java.util.logging.manager");
      if (loggingManagerClassName != null) {
        if (isClassLoaded(loggingManagerClassName, instrumentation)) {
          // Assuming that the logging manager (most likely org.jboss.logmanager.LogManager)
          // is loaded by the static initializer of java.util.logging.LogManager (and not by
          // other code), we know now that either the java.util.logging.LogManager singleton
          // is or will be initialized.
          // Here where trigger to for load the class:
          // https://github.com/jboss-modules/jboss-modules/blob/1.5.1.Final/src/main/java/org/jboss/modules/Main.java#L482
          // Therefore the steps 3-6 of the proposal for option 2 don't need to be performed,
          // see https://github.com/rhuss/jolokia/issues/258 for details.
          return;
        }
      }
      try {
        Thread.sleep(LOGGING_DETECT_INTERVAL);
        count++;
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalStateException(String.format("Detected JBoss Module loader, but property java.util.logging.manager is not set after %d seconds", LOGGING_DETECT_TIMEOUT / 1000));
  }

}
