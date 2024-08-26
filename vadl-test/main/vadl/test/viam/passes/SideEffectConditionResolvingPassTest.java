package vadl.test.viam.passes;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;

public class SideEffectConditionResolvingPassTest extends AbstractTest {

  @TestFactory
  Stream<DynamicTest> sideEffectConditionResolvingPass()
      throws IOException, DuplicatedPassKeyException {
    var config = new GeneralConfiguration("build/test-out", true);
    var setup = setupPassManagerAndRunSpec(
        "sideEffectConditionResolving/valid_test_cases.vadl",
        PassOrder.viam(config)
    );

    var spec = setup.specification();

    return Stream.of(
//        dynamicTest()
    );
  }

}
