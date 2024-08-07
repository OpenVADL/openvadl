package vadl.test;


import static vadl.test.TestUtils.findDefinitionByNameIn;

import org.junit.jupiter.api.Test;
import vadl.viam.Function;

/**
 * This test file corresponds to the temp_test.vadl in the test sources.
 * It is used to debug small problems without creating new test files.
 */
public class TemporaryTest extends AbstractTest {

  @Test
  public void temporaryTest() {

    var spec = runAndGetViamSpecification("temp_test.vadl");

  }

}
