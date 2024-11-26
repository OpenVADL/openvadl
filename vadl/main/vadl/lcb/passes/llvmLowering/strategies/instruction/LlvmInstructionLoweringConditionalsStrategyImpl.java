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
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionWrappedNode;
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
      List<TableGenPattern> patterns) {
    var result = new ArrayList<TableGenPattern>();
    var lts =
        supportedInstructions.getOrDefault(MachineInstructionLabel.LTS, Collections.emptyList());
    var ltus =
        supportedInstructions.getOrDefault(MachineInstructionLabel.LTU, Collections.emptyList());
    var ltis =
        supportedInstructions.getOrDefault(MachineInstructionLabel.LTIU, Collections.emptyList());
    var xors =
        supportedInstructions.getOrDefault(MachineInstructionLabel.XOR, Collections.emptyList());
    var xoris =
        supportedInstructions.getOrDefault(MachineInstructionLabel.XORI, Collections.emptyList());

    xors.stream().findFirst()
        .ifPresent(xor -> {
          ltis.stream().findFirst().ifPresent(lti -> {
            // Why `lts`? Because we need an initial pattern from which we construct a new pattern.
            // In that case, "less-than"
            if (lts.contains(instruction)) {
              eq(lti, xor, patterns, result);
            }
          });

          ltus.stream().findFirst().ifPresent(ltu -> {
            // Why `lts`? Because we need an initial pattern from which we construct a new pattern.
            // In that case, "less-than"
            if (lts.contains(instruction)) {
              neq(ltu, xor, patterns, result);
            }
          });
        });


    xoris.stream().findFirst()
        .ifPresent(xori -> {
          ltus.stream().findFirst().ifPresent(ltu ->
              ltis.stream().findFirst().ifPresent(lti -> {
                // Why `ltis`? Because we need an initial pattern from which we construct a new
                // pattern.
                // In that case, "less-than-immediate"
                if (ltis.contains(instruction)) {
                  neqWithImmediate(ltu, xori, patterns, result);
                }
              }));
        });

    return result;
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

              var newArgs = new LcbMachineInstructionWrappedNode(xor, node.arguments());
              node.setArgs(
                  new NodeList<>(newArgs, new ConstantNode(new Constant.Str("1"))));
            });

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

              var newArgs = new LcbMachineInstructionWrappedNode(xor, node.arguments());
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

              var newArgs = new LcbMachineInstructionWrappedNode(xori, node.arguments());
              node.setArgs(
                  new NodeList<>(zeroRegister, newArgs));
            });

        result.add(outputPattern);
      }
    }
  }
}
