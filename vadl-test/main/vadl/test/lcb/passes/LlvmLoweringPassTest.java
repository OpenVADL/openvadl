package vadl.test.lcb.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenMachineInstructionPrinterVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenPatternPrinterVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmLoweringPassTest extends AbstractLcbTest {

  record TestOutput(List<TableGenInstructionOperand> inputs,
                    List<TableGenInstructionOperand> outputs,
                    List<String> selectorPatterns,
                    List<String> machinePatterns,
                    LlvmLoweringPass.Flags flags,
                    boolean skipParameterIdentityCheck) {
  }

  private static final HashMap<String, TestOutput>
      expectedResults = new HashMap<>();

  private static final Node DUMMY_NODE = new ConstantNode(new Constant.Str(""));

  private static TestOutput createTestOutputRR(String dagNode,
                                               String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "X", "rs2")),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2)", dagNode)),
        List.of(String.format("(%s X:$rs1, X:$rs2)", machineInstruction)),
        createEmptyFlags(),
        false
    );
  }

  private static TestOutput createTestOutputRRWithConditional(LlvmCondCode condCode,
                                                              String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "X", "rs2")),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(%s X:$rs1, X:$rs2, %s)", "setcc", condCode)),
        List.of(String.format("(%s X:$rs1, X:$rs2)", machineInstruction)),
        createEmptyFlags(),
        false
    );
  }

  private static TestOutput createTestOutputRRWithConditionalBranch(LlvmCondCode condCode,
                                                                    String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "X", "rs2"),
            new TableGenInstructionOperand(DUMMY_NODE, "RV64IM_Btype_immAsLabel",
                "imm")),
        List.of(),
        List.of(
            String.format("(brcc %s, X:$rs1, X:$rs2, bb:$imm)",
                condCode),
            String.format(
                "(brcond (i64 (%s X:$rs1, X:$rs2)), bb:$imm)",
                condCode.name().toLowerCase())),
        // We have the same pattern twice because we have to selectors which emit the same
        // machine instruction.
        List.of(String.format("(%s X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)",
                machineInstruction),
            String.format("(%s X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)",
                machineInstruction)
        ),
        createBranchFlags(),
        true
    );
  }

  private static TestOutput createTestOutputAddI() {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "RV64IM_Itype_immAsInt64", "imm")),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(add X:$rs1, %s:$%s)", "RV64IM_Itype_immAsInt64",
                "imm"),
            String.format("(add AddrFI:$rs1, %s:$%s)", "RV64IM_Itype_immAsInt64",
                "imm")
        ),
        List.of(String.format("(%s X:$rs1, %s:$%s)", "ADDI",
                "RV64IM_Itype_immAsInt64",
                "imm"),
            String.format("(%s AddrFI:$rs1, %s:$%s)", "ADDI",
                "RV64IM_Itype_immAsInt64",
                "imm")
        ),
        createEmptyFlags(),
        false);
  }

  private static TestOutput createTestOutputRI(String immediateOperand,
                                               String immediateName,
                                               String dagNode,
                                               String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, immediateOperand, immediateName)),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(%s X:$rs1, %s:$%s)", dagNode, immediateOperand, immediateName)),
        List.of(String.format("(%s X:$rs1, %s:$%s)", machineInstruction, immediateOperand,
            immediateName)),
        createEmptyFlags(),
        false);
  }

  private static TestOutput createTestOutputRIWithConditional(String immediateOperand,
                                                              String immediateName,
                                                              LlvmCondCode condCode,
                                                              String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, immediateOperand, immediateName)),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(%s X:$rs1, %s:$%s, %s)", "setcc", immediateOperand, immediateName,
            condCode)),
        List.of(String.format("(%s X:$rs1, %s:$%s)", machineInstruction, immediateOperand,
            immediateName)),
        createEmptyFlags(),
        false
    );
  }

  private static TestOutput createTestOutputStoreMemory(String dagNode,
                                                        String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "X", "rs2"),
            new TableGenInstructionOperand(DUMMY_NODE, "RV64IM_Stype_immAsInt64", "imm")),
        List.of(),
        List.of(String.format("(%s X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm))",
                dagNode),
            String.format("(%s X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm))",
                dagNode),
            String.format("(%s X:$rs2, AddrFI:$rs1)",
                dagNode)
        ),
        List.of(String.format("(%s X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, X:$rs2, (i64 0))",
                machineInstruction)
        ),
        createStoreMemoryFlags(),
        false
    );
  }

  private static TestOutput createTestOutputLoadMemory(
      String typeNode,
      String dagNode,
      String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "RV64IM_Itype_immAsInt64", "imm")),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(%s (%s (add X:$rs1, RV64IM_Itype_immAsInt64:$imm)))",
                typeNode, dagNode),
            String.format("(%s (%s (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)))",
                typeNode, dagNode),
            String.format("(%s (%s AddrFI:$rs1))", typeNode, dagNode)
        ),
        List.of(String.format("(%s X:$rs1, RV64IM_Itype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, (i64 0))",
                machineInstruction)
        ),
        createLoadMemoryFlags(),
        false
    );
  }


  private static TestOutput createTestOutputLoadMemoryWithExt(
      String typeNode,
      String dagNode,
      String extNode,
      String machineInstruction) {
    return new TestOutput(
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "RV64IM_Itype_immAsInt64", "imm")),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        List.of(String.format("(%s (%s (add X:$rs1, RV64IM_Itype_immAsInt64:$imm)))",
                typeNode, dagNode),
            String.format("(%s (%s (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)))",
                typeNode, dagNode),
            String.format("(%s (%s (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)))", typeNode,
                extNode),
            String.format("(%s (%s AddrFI:$rs1))", typeNode, dagNode),
            String.format("(%s (%s AddrFI:$rs1))", typeNode, extNode)
        ),
        List.of(String.format("(%s X:$rs1, RV64IM_Itype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, (i64 0))",
                machineInstruction),
            String.format("(%s AddrFI:$rs1, (i64 0))",
                machineInstruction)
        ),
        createLoadMemoryFlags(),
        false
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
    expectedResults.put("MUL", createTestOutputRR("smullohi", "MUL"));
    expectedResults.put("XOR", createTestOutputRR("xor", "XOR"));
    expectedResults.put("AND", createTestOutputRR("and", "AND"));
    expectedResults.put("OR", createTestOutputRR("or", "OR"));
    expectedResults.put("ADDI",
        createTestOutputAddI());
    expectedResults.put("ORI",
        createTestOutputRI("RV64IM_Itype_immAsInt64", "imm", "or", "ORI"));
    expectedResults.put("ANDI",
        createTestOutputRI("RV64IM_Itype_immAsInt64", "imm", "and", "ANDI"));
    /*
    CONDITIONALS
     */
    expectedResults.put("SLT",
        createTestOutputRRWithConditional(LlvmCondCode.SETLT, "SLT"));
    expectedResults.put("SLTU",
        createTestOutputRRWithConditional(LlvmCondCode.SETULT, "SLTU"));
    expectedResults.put("SLTI",
        createTestOutputRIWithConditional("RV64IM_Itype_immAsInt64", "imm",
            LlvmCondCode.SETLT, "SLTI"));
    expectedResults.put("SLTUI",
        createTestOutputRIWithConditional("RV64IM_Btype_immAsInt64", "imm",
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
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rs1"),
            new TableGenInstructionOperand(DUMMY_NODE, "RV64IM_Itype_immAsInt64", "imm")),
        List.of(new TableGenInstructionOperand(DUMMY_NODE, "X", "rd")),
        Collections.emptyList(),
        Collections.emptyList(),
        createEmptyFlags(),
        false
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
    expectedResults.put("LB",
        createTestOutputLoadMemoryWithExt("i64", "sextloadi8", "extloadi8", "LB"));
    expectedResults.put("LH",
        createTestOutputLoadMemoryWithExt("i64", "sextloadi16", "extloadi16", "LH"));
    expectedResults.put("LW",
        createTestOutputLoadMemoryWithExt("i64", "sextloadi32", "extloadi32", "LW"));
    expectedResults.put("LD", createTestOutputLoadMemory("i64", "load", "LD"));
    expectedResults.put("LBU", createTestOutputLoadMemory("i64", "zextloadi8", "LBU"));
    expectedResults.put("LHU", createTestOutputLoadMemory("i64", "zextloadi16", "LHU"));
    expectedResults.put("LWU", createTestOutputLoadMemory("i64", "zextloadi32", "LWU"));
    expectedResults.put("LDU", createTestOutputLoadMemory("i64", "zextloadi64", "LDU"));
  }

  @TestFactory
  Stream<DynamicTest> testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(LlvmLoweringPass.class.getName() + "-1"));
    var passManager = setup.passManager();
    var spec = setup.specification();

    // When
    var
        llvmResults =
        (LlvmLoweringPass.LlvmLoweringPassResult)
            passManager.getPassResults()
                .lastResultOf(LlvmLoweringPass.class);

    // Then
    return spec.isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(x -> expectedResults.containsKey(x.identifier.simpleName()))
        .map(t -> DynamicTest.dynamicTest(t.identifier.simpleName(), () -> {
          var expectedTestOutput = expectedResults.get(t.identifier.simpleName());
          var res = llvmResults.machineInstructionRecords().get(t);
          Assertions.assertNotNull(res);

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
              .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
              .map(x -> (TableGenSelectionWithOutputPattern) x)
              .map(TableGenSelectionWithOutputPattern::machine)
              .flatMap(x -> x.getDataflowRoots().stream())
              .map(rootNode -> {
                var visitor = new TableGenMachineInstructionPrinterVisitor();
                visitor.visit((ExpressionNode) rootNode);
                return visitor.getResult();
              }).toList();
          Assertions.assertEquals(expectedTestOutput.machinePatterns,
              machinePatterns);

          // Flags
          Assertions.assertEquals(expectedTestOutput.flags(), res.flags());
        }));
  }
}
