package vadl.test;


import org.junit.jupiter.api.Test;

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
