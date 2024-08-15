package vadl.test.lcb.passes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.tablegen.lowering.TableGenPatternVisitor;
import vadl.lcb.tablegen.model.TableGenImmediateOperand;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.pass.PassKey;
import vadl.test.AbstractTest;
import vadl.viam.Instruction;
import vadl.viam.passes.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class LlvmLoweringPassTest extends AbstractTest {

  record TestOutput(List<TableGenInstructionOperand> inputs,
                    List<TableGenInstructionOperand> outputs,
                    List<String> patterns) {
  }

  private final static HashMap<String, TestOutput>
      expectedResults =
      new HashMap<>();

  private static TestOutput createTestOutputRR(String dagNode) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2)", dagNode)));
  }

  private static TestOutput createTestOutputRI(String immediateOperand,
                                               String immediateName,
                                               String dagNode) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand(immediateOperand, immediateName)),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, %s:$%s)", dagNode, immediateOperand, immediateName)));
  }

  static {
    expectedResults.put("ADD", createTestOutputRR("add"));
    expectedResults.put("SUB", createTestOutputRR("sub"));
    expectedResults.put("MUL", createTestOutputRR("smul_lohi"));
    expectedResults.put("XOR", createTestOutputRR("xor"));
    expectedResults.put("AND", createTestOutputRR("and"));
    expectedResults.put("OR", createTestOutputRR("or"));
    expectedResults.put("ADDI", createTestOutputRI("immS_decodeAsInt64", "immS", "add"));
    expectedResults.put("ORI", createTestOutputRI("immS_decodeAsInt64", "immS", "or"));
    expectedResults.put("ANDI", createTestOutputRI("immS_decodeAsInt64", "immS", "and"));
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
          var patterns = res.patterns().stream()
              .map(pattern -> pattern.getNodes().filter(x -> x.usageCount() == 0).findFirst())
              .filter(Optional::isPresent)
              .map(rootNode -> {
                var visitor = new TableGenPatternVisitor();
                visitor.visit(rootNode.get());
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedResults.get(t.identifier.simpleName()).patterns,
              patterns);
        }));
  }

}
