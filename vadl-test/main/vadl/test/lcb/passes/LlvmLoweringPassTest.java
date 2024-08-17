package vadl.test.lcb.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.model.LlvmCondCode;
import vadl.lcb.tablegen.lowering.TableGenPatternVisitor;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.pass.PassKey;
import vadl.test.AbstractTest;
import vadl.viam.Instruction;
import vadl.viam.passes.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class LlvmLoweringPassTest extends AbstractTest {

  record TestOutput(List<TableGenInstructionOperand> inputs,
                    List<TableGenInstructionOperand> outputs,
                    List<String> selectorPatterns,
                    List<String> machinePatterns) {
  }

  private static final HashMap<String, TestOutput>
      expectedResults =
      new HashMap<>();

  private static TestOutput createTestOutputRR(String dagNode, String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2)", dagNode)),
        List.of(String.format("(%s X:$rs1, X:$rs2)", machineInstruction))
    );
  }

  private static TestOutput createTestOutputRRWithConditional(LlvmCondCode condCode,
                                                              String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2, %s)", "setcc", condCode)),
        List.of(String.format("(%s X:$rs1, X:$rs2)", machineInstruction))
    );
  }

  private static TestOutput createTestOutputRRWithConditionalBranch(LlvmCondCode condCode,
                                                                    String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2"),
            new TableGenInstructionOperand("immS_decodeAsInt64", "immS")),
        List.of(),
        List.of(
            String.format("(%s (%s X:$rs1, X:$rs2), immS_decodeAsInt64:$immS)", "brcc", condCode)),
        List.of(String.format("(%s X:$rs1, X:$rs2, immS_decodeAsInt64:$immS)", machineInstruction))
    );
  }

  private static TestOutput createTestOutputRI(String immediateOperand,
                                               String immediateName,
                                               String dagNode,
                                               String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand(immediateOperand, immediateName)),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, %s:$%s)", dagNode, immediateOperand, immediateName)),
        List.of(String.format("(%s X:$rs1, %s:$%s)", machineInstruction, immediateOperand,
            immediateName)));
  }

  private static TestOutput createTestOutputRIWithConditional(String immediateOperand,
                                                              String immediateName,
                                                              LlvmCondCode condCode,
                                                              String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand(immediateOperand, immediateName)),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, %s:$%s, %s)", "setcc", immediateOperand, immediateName,
            condCode)),
        List.of(String.format("(%s X:$rs1, %s:$%s)", machineInstruction, immediateOperand,
            immediateName))
    );
  }

  static {
    /*
    ARITHMETIC AND LOGIC
     */
    expectedResults.put("ADD", createTestOutputRR("add", "ADD"));
    expectedResults.put("SUB", createTestOutputRR("sub", "SUB"));
    expectedResults.put("MUL", createTestOutputRR("smul_lohi", "MUL"));
    expectedResults.put("XOR", createTestOutputRR("xor", "XOR"));
    expectedResults.put("AND", createTestOutputRR("and", "AND"));
    expectedResults.put("OR", createTestOutputRR("or", "OR"));
    expectedResults.put("ADDI", createTestOutputRI("immS_decodeAsInt64", "immS", "add", "ADDI"));
    expectedResults.put("ORI", createTestOutputRI("immS_decodeAsInt64", "immS", "or", "ORI"));
    expectedResults.put("ANDI", createTestOutputRI("immS_decodeAsInt64", "immS", "and", "ANDI"));
    /*
    CONDITIONALS
     */
    expectedResults.put("SLT",
        createTestOutputRRWithConditional(LlvmCondCode.SETLT, "SLT"));
    expectedResults.put("SLTU",
        createTestOutputRRWithConditional(LlvmCondCode.SETULT, "SLTU"));
    expectedResults.put("SLTI",
        createTestOutputRIWithConditional("immS_decodeAsInt64", "immS",
            LlvmCondCode.SETLT, "SLTI"));
    expectedResults.put("SLTUI",
        createTestOutputRIWithConditional("immS_decodeAsInt64", "immS",
            LlvmCondCode.SETULT, "SLTUI"));
    /*
    CONDITIONAL BRANCHES
     */
    expectedResults.put("BEQ", createTestOutputRRWithConditionalBranch(LlvmCondCode.SETEQ, "BEQ"));
    expectedResults.put("BGE", createTestOutputRRWithConditionalBranch(LlvmCondCode.SETGE, "BGE"));
    expectedResults.put("BGEU",
        createTestOutputRRWithConditionalBranch(LlvmCondCode.SETUGE, "BGEU"));
    expectedResults.put("BLT", createTestOutputRRWithConditionalBranch(LlvmCondCode.SETLT, "BLT"));
    expectedResults.put("BLTU",
        createTestOutputRRWithConditionalBranch(LlvmCondCode.SETULT, "BLTU"));
    expectedResults.put("BNE", createTestOutputRRWithConditionalBranch(LlvmCondCode.SETNE, "BNE"));
    /*
    INDIRECT CALL
     */
    expectedResults.put("JALR", new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("immS_decodeAsInt64", "immS")),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList()
    ));
  }

  @TestFactory
  Stream<DynamicTest> testLowering() throws IOException {
    // Given
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var passResults = new HashMap<PassKey, Object>();

    new TypeCastEliminationPass().execute(passResults, spec);
    passResults.put(new PassKey(FunctionInlinerPass.class.toString()),
        new FunctionInlinerPass().execute(passResults, spec));
    passResults.put(new PassKey(IsaMatchingPass.class.toString()),
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
          var selectorPatterns = res.patterns().stream()
              .map(LlvmLoweringPass.LlvmLoweringTableGenPattern::selector)
              .map(pattern -> pattern.getNodes().filter(x -> x.usageCount() == 0).findFirst())
              .filter(Optional::isPresent)
              .map(rootNode -> {
                var visitor = new TableGenPatternVisitor();
                visitor.visit(rootNode.get());
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedResults.get(t.identifier.simpleName()).selectorPatterns,
              selectorPatterns);
          var machinePatterns = res.patterns().stream()
              .map(LlvmLoweringPass.LlvmLoweringTableGenPattern::machine)
              .map(pattern -> pattern.getNodes().filter(x -> x.usageCount() == 0).findFirst())
              .filter(Optional::isPresent)
              .map(rootNode -> {
                var visitor = new TableGenPatternVisitor();
                visitor.visit(rootNode.get());
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedResults.get(t.identifier.simpleName()).machinePatterns,
              machinePatterns);
        }));
  }

}
