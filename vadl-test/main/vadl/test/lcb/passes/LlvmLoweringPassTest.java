package vadl.test.lcb.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.model.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenMachineInstructionPrinterVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenPatternPrinterVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.viam.Instruction;

public class LlvmLoweringPassTest extends AbstractLcbTest {

  record TestOutput(List<TableGenInstructionOperand> inputs,
                    List<TableGenInstructionOperand> outputs,
                    List<String> selectorPatterns,
                    List<String> machinePatterns,
                    LlvmLoweringPass.Flags flags) {
  }

  private static final HashMap<String, TestOutput>
      expectedResults = new HashMap<>();

  private static TestOutput createTestOutputRR(String dagNode,
                                               String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2)", dagNode)),
        List.of(String.format("(%s X:$rs1, X:$rs2)", machineInstruction)),
        createEmptyFlags()
    );
  }

  private static TestOutput createTestOutputRRWithConditional(LlvmCondCode condCode,
                                                              String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2, %s)", "setcc", condCode)),
        List.of(String.format("(%s X:$rs1, X:$rs2)", machineInstruction)),
        createEmptyFlags()
    );
  }

  private static TestOutput createTestOutputRRWithConditionalBranch(LlvmCondCode condCode,
                                                                    String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2"),
            new TableGenInstructionOperand("RV3264I_Btype_immS_decodeAsInt64", "immS")),
        List.of(),
        List.of(
            String.format("(brcc (%s X:$rs1, X:$rs2), RV3264I_Btype_immS_decodeAsInt64:$immS)",
                condCode),
            String.format(
                "(brcond (i32 (setcc X:$rs1, X:$rs2, %s)), RV3264I_Btype_immS_decodeAsInt64:$immS)",
                condCode)),
        // We have the same pattern twice because we have to selectors which emit the same
        // machine instruction.
        List.of(String.format("(%s X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS)",
                machineInstruction),
            String.format("(%s X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS)",
                machineInstruction)
        ),
        createBranchFlags()
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
            immediateName)),
        createEmptyFlags());
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
            immediateName)),
        createEmptyFlags()
    );
  }

  private static TestOutput createTestOutputStoreMemory(String dagNode,
                                                        String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("X", "rs2"),
            new TableGenInstructionOperand("RV3264I_Stype_immS_decodeAsInt64", "immS")),
        List.of(),
        List.of(String.format("(%s X:$rs2, (add X:$rs1, RV3264I_Stype_immS_decodeAsInt64:$immS))",
            dagNode)),
        List.of(String.format("(%s X:$rs1, X:$rs2, RV3264I_Stype_immS_decodeAsInt64:$immS)",
            machineInstruction)),
        createStoreMemoryFlags()
    );
  }

  private static TestOutput createTestOutputLoadMemory(
      String typeNode,
      String dagNode,
      String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand("X", "rs1"),
            new TableGenInstructionOperand("RV3264I_Itype_immS_decodeAsInt64", "immS")),
        List.of(new TableGenInstructionOperand("X", "rd")),
        List.of(String.format("(%s (%s (add X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS)))",
            typeNode, dagNode)),
        List.of(String.format("(%s X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS)",
            machineInstruction)),
        createLoadMemoryFlags()
    );
  }

  private static LlvmLoweringPass.Flags createEmptyFlags() {
    return LlvmLoweringPass.Flags.empty();
  }

  private static LlvmLoweringPass.Flags createBranchFlags() {
    return new LlvmLoweringPass.Flags(true, true, false, false, false, false, false, false);
  }

  private static LlvmLoweringPass.Flags createStoreMemoryFlags() {
    return new LlvmLoweringPass.Flags(false, false, false, false, false, false, false, true);
  }

  private static LlvmLoweringPass.Flags createLoadMemoryFlags() {
    return new LlvmLoweringPass.Flags(false, false, false, false, false, false, true, false);
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
    expectedResults.put("ADDI",
        createTestOutputRI("RV3264I_Itype_immS_decodeAsInt64", "immS", "add", "ADDI"));
    expectedResults.put("ORI",
        createTestOutputRI("RV3264I_Itype_immS_decodeAsInt64", "immS", "or", "ORI"));
    expectedResults.put("ANDI",
        createTestOutputRI("RV3264I_Itype_immS_decodeAsInt64", "immS", "and", "ANDI"));
    /*
    CONDITIONALS
     */
    expectedResults.put("SLT",
        createTestOutputRRWithConditional(LlvmCondCode.SETLT, "SLT"));
    expectedResults.put("SLTU",
        createTestOutputRRWithConditional(LlvmCondCode.SETULT, "SLTU"));
    expectedResults.put("SLTI",
        createTestOutputRIWithConditional("RV3264I_Itype_immS_decodeAsInt64", "immS",
            LlvmCondCode.SETLT, "SLTI"));
    expectedResults.put("SLTUI",
        createTestOutputRIWithConditional("RV3264I_Btype_immS_decodeAsInt64", "immS",
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
            new TableGenInstructionOperand("RV3264I_Itype_immS_decodeAsInt64", "immS")),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        createEmptyFlags()
    ));
    /*
    MEMORY STORE
     */
    expectedResults.put("SB", createTestOutputStoreMemory("truncstorei8", "SB"));
    expectedResults.put("SH", createTestOutputStoreMemory("truncstorei16", "SH"));
    expectedResults.put("SW", createTestOutputStoreMemory("truncstorei32", "SW"));
    expectedResults.put("SD", createTestOutputStoreMemory("store", "SD"));
    /*
    MEMORY LOAD
     */
    expectedResults.put("LB", createTestOutputLoadMemory("i64", "sextloadi8", "LB"));
    expectedResults.put("LH", createTestOutputLoadMemory("i64", "sextloadi16", "LH"));
    expectedResults.put("LW", createTestOutputLoadMemory("i64", "sextloadi32", "LW"));
    expectedResults.put("LD", createTestOutputLoadMemory("i64", "load", "LD"));
    expectedResults.put("LBU", createTestOutputLoadMemory("u64", "zextloadi8", "LBU"));
    expectedResults.put("LHU", createTestOutputLoadMemory("u64", "zextloadi16", "LHU"));
    expectedResults.put("LWU", createTestOutputLoadMemory("u64", "zextloadi32", "LWU"));
    expectedResults.put("LDU", createTestOutputLoadMemory("u64", "zextloadi64", "LDU"));
  }

  @TestFactory
  Stream<DynamicTest> testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "examples/rv3264im.vadl",
        new PassKey(LlvmLoweringPass.class.getName()));
    var passManager = setup.passManager();
    var spec = setup.specification();

    // When
    IdentityHashMap<Instruction, LlvmLoweringPass.LlvmLoweringIntermediateResult>
        llvmResults =
        (IdentityHashMap<Instruction, LlvmLoweringPass.LlvmLoweringIntermediateResult>)
            passManager.getPassResults()
                .get(new PassKey(LlvmLoweringPass.class.getName()));

    // Then
    return spec.isas().flatMap(x -> x.instructions().stream())
        .filter(x -> expectedResults.containsKey(x.identifier.simpleName()))
        .map(t -> DynamicTest.dynamicTest(t.identifier.simpleName(), () -> {
          var expectedTestOutput = expectedResults.get(t.identifier.simpleName());
          var res = llvmResults.get(t);
          Assertions.assertNotNull(res);
          // Inputs
          Assertions.assertEquals(expectedTestOutput.inputs(),
              res.inputs());
          // Outputs
          Assertions.assertEquals(expectedTestOutput.outputs(),
              res.outputs());

          // Selector Patterns
          var selectorPatterns = res.patterns().stream()
              .map(TableGenPattern::selector)
              .flatMap(x -> x.getDataflowRoots().stream())
              .map(rootNode -> {
                var visitor = new TableGenPatternPrinterVisitor();
                visitor.visit(rootNode);
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedTestOutput.selectorPatterns,
              selectorPatterns);

          // Machine Patterns
          var machinePatterns = res.patterns().stream()
              .map(TableGenPattern::machine)
              .flatMap(x -> x.getDataflowRoots().stream())
              .map(rootNode -> {
                var visitor = new TableGenMachineInstructionPrinterVisitor();
                visitor.visit(rootNode);
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedTestOutput.machinePatterns,
              machinePatterns);

          // Flags
          Assertions.assertEquals(expectedTestOutput.flags(), res.flags());
        }));
  }

}
