package com.heroku.agent.metrics.detector;

import org.junit.Before;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class JBossDetectorTest {

  private JBossDetector detector;

  @Before
  public void setup() {
    detector = new JBossDetector();
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyIsClassLoadedArgumentChecksNullInstrumentation() {
    detector.isClassLoaded("xx", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyIsClassLoadedArgumentChecks2NullClassname() {
    detector.isClassLoaded(null, mock(Instrumentation.class));
  }

  @Test
  public void verifyIsClassLoadedNotLoaded() {
    Instrumentation inst = mock(Instrumentation.class);
    when(inst.getAllLoadedClasses()).thenReturn(new Class[] {});
    assertFalse(detector.isClassLoaded("org.Dummy", inst));
    verify(inst, times(1)).getAllLoadedClasses();
  }

  @Test
  public void verifyIsClassLoadedLoaded() {
    Instrumentation inst = mock(Instrumentation.class);
    when(inst.getAllLoadedClasses()).thenReturn(new Class[] {JBossDetectorTest.class});
    assertTrue(detector.isClassLoaded(JBossDetectorTest.class.getName(), inst));
    verify(inst, times(1)).getAllLoadedClasses();
  }

  @Test
  public void verifyJvmAgentStartup() throws MalformedURLException {
    Instrumentation inst = mock(Instrumentation.class);
    when(inst.getAllLoadedClasses()).
        thenReturn(new Class[] {}).
        thenReturn(new Class[] {}).
        thenReturn(new Class[] {}).
        thenReturn(new Class[] {JBossDetectorTest.class});
    ClassLoader cl = mock(ClassLoader.class);
    when(cl.getResource("org/jboss/modules/Main.class")).thenReturn(new URL("http", "dummy", ""));
    String prevPkgValue = System.setProperty("jboss.modules.system.pkgs", "blah");
    String prevLogValue = System.setProperty("java.util.logging.manager", JBossDetectorTest.class.getName());

    try {
      detector.jvmAgentStartup(inst, cl);

      verify(inst, atLeast(3)).getAllLoadedClasses();
      verify(cl, atLeastOnce()).getResource("org/jboss/modules/Main.class");
    } finally {
      resetSysProp(prevLogValue, "java.util.logging.manager");
      resetSysProp(prevPkgValue, "jboss.modules.system.pkgs");
    }
  }

  private void resetSysProp(String prevValue, String key) {
    if (prevValue == null) {
      System.getProperties().remove(key);
    } else {
      System.setProperty(key, prevValue);
    }
  }
}
