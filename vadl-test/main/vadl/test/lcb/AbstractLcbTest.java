package vadl.test.lcb;

import vadl.lcb.config.LcbConfiguration;
import vadl.test.AbstractTest;

public abstract class AbstractLcbTest extends AbstractTest {
  /**
   * Returns a dummy configuration.
   */
  public static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("dummyValue");
  }
}
