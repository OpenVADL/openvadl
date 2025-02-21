package vadl.lcb.passes.llvmLowering.strategies.instruction.conditionals;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.types.BuiltInTable;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Lowering of less-than-signed conditionals into TableGen.
 */
public class LlvmInstructionLoweringLessThanSignedConditionalsStrategyImpl
    extends LlvmInstructionLoweringStrategy {

  private final Set<MachineInstructionLabel> supported =
      Set.of(MachineInstructionLabel.LTS);

  public LlvmInstructionLoweringLessThanSignedConditionalsStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return this.supported;
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithDefaultFieldAccessReplacement();
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi) {
    var result = new ArrayList<TableGenPattern>();

    /*
     * Get the instructions to generate new instructions patterns for `instruction`/
     */
    var lti = getFirst(instruction, supportedInstructions.labels(), MachineInstructionLabel.LTIU);
    var xor = getFirst(instruction, supportedInstructions.labels(), MachineInstructionLabel.XOR);
    var xori = getFirst(instruction, supportedInstructions.labels(), MachineInstructionLabel.XORI);

    eq(lti, xor, patterns, result);
    gt(patterns, result);
    leq(xori, patterns, result);
    geq(xori, patterns, result);

    return result;
  }

  /**
   * This function will iterate over all the patterns and tries to find register-register. It sets
   * the builtin to {@link BuiltInTable#SGTH}. It then copies the pattern swaps the arguments.
   */
  private void gt(List<TableGenPattern> patterns,
                  List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< (setcc X:$rs1, X:$rs2, SETGT ),
                  (SLT X:$rs2, X:$rs1)>;
    */

    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.SGTH);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(LlvmCondCode.SETGT.name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              // Swap the operands
              Collections.reverse(node.arguments());
            });

        result.add(outputPattern);
      }
    }
  }

  private Instruction getFirst(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      MachineInstructionLabel label) {
    return ensurePresent(supportedInstructions.getOrDefault(label, Collections.emptyList())
            .stream().findFirst(),
        () -> Diagnostic.error(String.format("No instruction with label '%s' detected.", label),
            instruction.sourceLocation()));
  }

  /**
   * This method iterates over the given patterns. When a pattern is register-register. It sets the
   * condition to {@link BuiltInTable#EQU} then construct a new pattern. The machine instruction
   * will be replaced by {@code sltiu} and {@code xor}. The second parameter of {@code sltiu}
   * will be {@code 1}. Logically it means that we check whether the two registers are equal. If
   * they are then the result of the XOR operation is zero. So to be equal, it must be less than 1.
   */
  private void eq(Instruction sltiu,
                  Instruction xor,
                  List<TableGenPattern> patterns,
                  List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETEQ ),
                   ( SLTIU ( XOR X:$rs1, X:$rs2 ), 1 ) >;
               */
    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.EQU);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              node.setOutputInstruction(sltiu);

              var newArgs = new LcbMachineInstructionNode(node.arguments(), xor);
              node.setArgs(
                  new NodeList<>(newArgs, new ConstantNode(new Constant.Str("1"))));
            });

        result.add(outputPattern);
      }
    }
  }

  /**
   * Goes over all patterns and tries to find a register-register pattern.
   * It sets the builtin to {@link BuiltInTable#SGEQ}.
   * It then wraps {@code xori} over the {@code slt} pattern.
   */
  private void geq(Instruction xori,
                   List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETGE ),
                   ( XORI ( SLT X:$rs1, X:$rs2 ), 1 ) >;
               */
    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.SGEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        var machineInstruction =
            outputPattern.machine().getNodes(LcbMachineInstructionNode.class).findFirst().get();
        var newMachineInstruction = new LcbMachineInstructionNode(
            new NodeList<>(machineInstruction, new ConstantNode(new Constant.Str("1"))), xori);
        outputPattern.machine().addWithInputs(newMachineInstruction);

        result.add(outputPattern);
      }
    }
  }

  /**
   * Goes over all patterns and tries to find a register-register pattern.
   * It sets the builtin to {@link BuiltInTable#SLEQ}.
   * It then wraps {@code xori} over the {@code slt} pattern.
   */
  private void leq(Instruction xori,
                   List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETLE ),
                   ( XORI ( SLT X:$rs1, X:$rs2 ), 1 ) >;
               */
    for (var pattern : patterns) {
      var copy = pattern.copy();

      if (copy instanceof TableGenSelectionWithOutputPattern outputPattern) {
        // Change condition code
        var setcc = ensurePresent(
            outputPattern.selector().getNodes(LlvmSetccSD.class).toList().stream()
                .findFirst(),
            () -> Diagnostic.error("No setcc node was found", pattern.selector()
                .sourceLocation()));
        // Only RR and not RI should be replaced here.
        if (setcc.arguments().size() > 2
            && setcc.arguments().get(0) instanceof LlvmReadRegFileNode
            && setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.SLEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        var machineInstruction =
            outputPattern.machine().getNodes(LcbMachineInstructionNode.class).findFirst().get();
        Collections.reverse(machineInstruction.arguments());
        var newMachineInstruction = new LcbMachineInstructionNode(
            new NodeList<>(machineInstruction, new ConstantNode(new Constant.Str("1"))), xori);
        outputPattern.machine().addWithInputs(newMachineInstruction);

        result.add(outputPattern);
      }
    }
  }
}
