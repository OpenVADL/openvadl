package vadl.test.viam.passes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.error.DiagnosticList;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.passes.DuplicateWriteDetectionPass;

public class DuplicateWriteDetectionPassTest extends AbstractTest {

  static Stream<Arguments> invalidTestArgs() {
    var regErrMsg = "Register is written twice";
    var regFileErrMsg = "Register in register file is written twice";
    var memErrMsg = "Memory address is written twice";
    return Stream.of(
        of("reg_single_branch", 1, regErrMsg),
        of("reg_dual_branch", 1, regErrMsg),
        of("reg_triple_branch", 1, regErrMsg),
        of("reg_potential_branch", 1, regErrMsg),
        of("regfile_1", 1, regFileErrMsg),
        of("regfile_2", 1, regFileErrMsg),
        of("regfile_3", 2, regFileErrMsg),
        of("regfile_4", 1, regFileErrMsg),
        of("mem_1", 1, memErrMsg),
        of("mem_2", 1, memErrMsg),
        of("mem_3", 2, memErrMsg),
        of("mem_4", 1, memErrMsg)

    );
  }

  static Stream<Arguments> validTestArgs() {
    var tests = findAllTestSources("passes/singleResourceWriteValidation/valid_");
    return tests.stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("invalidTestArgs")
  void testInvalid(String name, int numErrs, String errmsg)
      throws IOException, DuplicatedPassKeyException {

    var err = assertThrows(DiagnosticList.class, () -> setupPassManagerAndRunSpec(
        "passes/singleResourceWriteValidation/invalid_" + name + ".vadl",
        PassOrders.viam(getConfiguration(false))
            .untilFirst(DuplicateWriteDetectionPass.class)
    ));

    assertEquals(numErrs, err.items.size());
    for (var item : err.items) {
      assertThat(item.getMessage(), containsString(errmsg));
    }
  }


  @ParameterizedTest
  @MethodSource("validTestArgs")
  void validTest(String test) throws IOException, DuplicatedPassKeyException {
    setupPassManagerAndRunSpec(
        test,
        PassOrders.viam(getConfiguration(false))
            .untilFirst(DuplicateWriteDetectionPass.class)
            .addDump("test-weird")
    );
  }

}
