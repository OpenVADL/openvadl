package vadl.test.lcb.passes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.pass.PassKey;
import vadl.test.AbstractTest;
import vadl.viam.Instruction;
import vadl.viam.passes.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class LlvmLoweringPassTest extends AbstractTest {

  private final static HashMap<String, LlvmLoweringPass.LlvmLoweringIntermediateResult>
      expectedResults =
      new HashMap<>();

  static {
    expectedResults.put("ADD", new LlvmLoweringPass.LlvmLoweringIntermediateResult(
        null,
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of()
    ));
  }

  @TestFactory
  Stream<DynamicTest> testLowering() throws IOException {
    // Given
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var passResults = new HashMap<PassKey, Object>();

    new TypeCastEliminationPass().execute(passResults, spec);
    passResults.put(new PassKey("FunctionInlinerPass"),
        new FunctionInlinerPass().execute(passResults, spec));
    passResults.put(new PassKey("IsaMatchingPass"),
        new IsaMatchingPass().execute(passResults, spec));

    // When
    var
        llvmResults =
        (Map<Instruction, LlvmLoweringPass.LlvmLoweringIntermediateResult>) new LlvmLoweringPass()
            .execute(passResults, spec);

    // Then
    return spec.isas().flatMap(x -> x.instructions().stream())
        .filter(x -> expectedResults.containsKey(x.identifier.simpleName()))
        .map(t -> DynamicTest.dynamicTest(t.identifier.simpleName(), () -> {
          var res = llvmResults.get(t);
          Assertions.assertNotNull(res);
          Assertions.assertEquals(expectedResults.get(t.identifier.simpleName()).inputs(),
              res.inputs());
          Assertions.assertEquals(expectedResults.get(t.identifier.simpleName()).outputs(),
              res.outputs());
        }));
  }

}
