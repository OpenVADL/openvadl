package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
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
    var ltis =
        supportedInstructions.getOrDefault(MachineInstructionLabel.LTIU, Collections.emptyList());
    var xors =
        supportedInstructions.getOrDefault(MachineInstructionLabel.XOR, Collections.emptyList());

    xors.stream().findFirst()
        .ifPresent(xor ->
            ltis.stream().findFirst().ifPresent(lti -> {
              if (lts.contains(instruction)) {
                eq(lti, xor, patterns, result);
              }
            }));
    return result;
  }

  private void eq(Instruction lti,
                  Instruction xor,
                  List<TableGenPattern> patterns,
                  ArrayList<TableGenPattern> result) {
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
        if (setcc.arguments().size() > 2 &&
            setcc.arguments().get(0) instanceof LlvmReadRegFileNode &&
            setcc.arguments().get(1) instanceof LlvmReadRegFileNode) {
          setcc.setBuiltIn(BuiltInTable.EQU);
        } else {
          // Otherwise, stop and go to next pattern.
          continue;
        }

        // Change machine instruction to immediate
        outputPattern.machine().getNodes(LcbMachineInstructionNode.class)
            .forEach(node -> {
              node.setInstruction(lti);

              var newArgs = new LcbMachineInstructionWrappedNode(xor, node.arguments());
              node.setArgs(
                  new NodeList<>(newArgs, new ConstantNode(new Constant.Value.Str("1"))));
            });

        result.add(outputPattern);
      }
    }
  }
}
