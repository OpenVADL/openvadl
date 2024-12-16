package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Lowering of conditionals into TableGen.
 */
public class LlvmInstructionLoweringConditionalsStrategyImpl
    extends LlvmInstructionLoweringStrategy {

  private final Set<MachineInstructionLabel> supported =
      Set.of(MachineInstructionLabel.LTS,
          MachineInstructionLabel.LTU,
          MachineInstructionLabel.LTIU);

  public LlvmInstructionLoweringConditionalsStrategyImpl(
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
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      DummyAbi abi) {
    var result = new ArrayList<TableGenPattern>();

    var flipped = LlvmLoweringPass.flipMachineInstructions(supportedInstructions);
    var label = flipped.get(instruction);

    var lti = getFirst(instruction, supportedInstructions, MachineInstructionLabel.LTIU);
    var ltu = getFirst(instruction, supportedInstructions, MachineInstructionLabel.LTU);
    var xor = getFirst(instruction, supportedInstructions, MachineInstructionLabel.XOR);
    var xori = getFirst(instruction, supportedInstructions, MachineInstructionLabel.XORI);

    if (label == MachineInstructionLabel.LTS) {
      // We use the `patterns` from `LTS` because it has two registers in the `patterns`.
      eq(lti, xor, patterns, result);
      gtOrUgt(instruction, patterns, result, BuiltInTable.SGTH, LlvmCondCode.SETGT);
      leqOrUleq(xori, patterns, result, BuiltInTable.SLEQ);
    } else if (label == MachineInstructionLabel.LTU) {
      neq(ltu, xor, patterns, result);
      uge(xori, patterns, result);
      gtOrUgt(instruction, patterns, result, BuiltInTable.SGTH, LlvmCondCode.SETUGT);
      leqOrUleq(xori, patterns, result, BuiltInTable.ULEQ);
    } else if (label == MachineInstructionLabel.LTIU) {
      neqWithImmediate(ltu, xori, patterns, result);
    }

    return result;
  }

  private void gtOrUgt(Instruction lt,
                       List<TableGenPattern> patterns,
                       List<TableGenPattern> result,
                       BuiltInTable.BuiltIn builtIn,
                       LlvmCondCode condCode) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< (setcc X:$rs1, X:$rs2, SETGT ),
                  (SLT X:$rs2, X:$rs1)>;

              or

              def : Pat<(setcc X:$rs1, X:$rs2, SETULT),
                  (SLTU X:$rs1, X:$rs2)>;

                  to

              def : Pat< (setcc X:$rs1, X:$rs2, SETUGT ),
                  (SLTU X:$rs2, X:$rs1)>;
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
          setcc.setBuiltIn(builtIn);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(condCode.name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              node.setInstruction(lt);
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

  private void eq(Instruction basePattern,
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
              node.setInstruction(basePattern);

              var newArgs = new LcbMachineInstructionNode(node.arguments(), xor);
              node.setArgs(
                  new NodeList<>(newArgs, new ConstantNode(new Constant.Str("1"))));
            });

        result.add(outputPattern);
      }
    }
  }


  private void leqOrUleq(Instruction xori,
                         List<TableGenPattern> patterns,
                         List<TableGenPattern> result,
                         BuiltInTable.BuiltIn builtIn) {
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
          setcc.setBuiltIn(builtIn);
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

  private void uge(Instruction xori,
                   List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLTU),
                  (SLTU X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETUGE ),
                   ( XORI ( SLTU X:$rs1, X:$rs2 ), 1 ) >;
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
          setcc.setBuiltIn(BuiltInTable.UGEQ);
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

  private void neq(Instruction basePattern,
                   Instruction xor,
                   List<TableGenPattern> patterns,
                   List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, X:$rs2, SETNE ),
                  ( SLTU X0, ( XOR X:$rs1, X:$rs2 ) ) >;
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
          setcc.setBuiltIn(BuiltInTable.NEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              node.setInstruction(basePattern);

              var registerFile =
                  ensurePresent(
                      xor.behavior().getNodes(ReadRegFileNode.class).map(
                              ReadRegFileNode::registerFile)
                          .findFirst(),
                      () -> Diagnostic.error("Cannot find a register", xor.sourceLocation()));

              var zeroConstraint =
                  ensurePresent(
                      Arrays.stream(registerFile.constraints())
                          .filter(x -> x.value().intValue() == 0)
                          .findFirst(),
                      () -> Diagnostic.error("Cannot find zero register for register file",
                          registerFile.sourceLocation()));
              // Cannot construct a `ReadReg` because this register does not really exist.
              // (for the VIAM spec)
              var zeroRegister = new ConstantNode(
                  new Constant.Str(
                      registerFile.simpleName() + zeroConstraint.address().intValue()));

              var newArgs = new LcbMachineInstructionNode(node.arguments(), xor);
              node.setArgs(
                  new NodeList<>(zeroRegister, newArgs));
            });

        result.add(outputPattern);
      }
    }
  }


  private void neqWithImmediate(Instruction machineInstructionToBeEmitted,
                                Instruction xori,
                                List<TableGenPattern> patterns,
                                List<TableGenPattern> result) {
    /*
              def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                  (SLT X:$rs1, X:$rs2)>;

                  to

              def : Pat< ( setcc X:$rs1, RV64IM_Itype_immAsInt64:$imm, SETNE ),
                  (SLTU X0, (XORI X:$rs1, RV64IM_Itype_immAsInt64:$imm) ) >;
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
            && setcc.arguments().get(1) instanceof FieldAccessRefNode) {
          setcc.setBuiltIn(BuiltInTable.NEQ);
          setcc.arguments().set(2,
              new ConstantNode(new Constant.Str(setcc.llvmCondCode().name())));
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              node.setInstruction(machineInstructionToBeEmitted);

              var registerFile =
                  ensurePresent(
                      xori.behavior().getNodes(ReadRegFileNode.class).map(
                              ReadRegFileNode::registerFile)
                          .findFirst(),
                      () -> Diagnostic.error("Cannot find a register", xori.sourceLocation()));

              var zeroConstraint =
                  ensurePresent(
                      Arrays.stream(registerFile.constraints())
                          .filter(x -> x.value().intValue() == 0)
                          .findFirst(),
                      () -> Diagnostic.error("Cannot find zero register for register file",
                          registerFile.sourceLocation()));
              // Cannot construct a `ReadReg` because this register does not really exist.
              // (for the VIAM spec)
              var zeroRegister = new ConstantNode(
                  new Constant.Str(
                      registerFile.simpleName() + zeroConstraint.address().intValue()));

              var newArgs = new LcbMachineInstructionNode(node.arguments(), xori);
              node.setArgs(
                  new NodeList<>(zeroRegister, newArgs));
            });

        result.add(outputPattern);
      }
    }
  }
}
