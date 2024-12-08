package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmTargetCallSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionRegisterFileOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstExpansionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.passes.dummyAbi.DummyAbi;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Generates the {@link LlvmLoweringRecord} for {@link MachineInstructionLabel#JALR}
 * instruction.
 */
public class LlvmInstructionLoweringIndirectJumpStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringIndirectJumpStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(MachineInstructionLabel.JALR);
  }

  @Override
  protected Optional<LlvmLoweringRecord> lowerInstruction(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      DummyAbi abi) {
    var copy = unmodifiedBehavior.copy();
    var visitor = replacementHooksWithDefaultFieldAccessReplacement();

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    var outputOperands = getTableGenOutputOperands(copy);
    var inputOperands = getTableGenInputOperands(outputOperands, copy);

    var uses = getRegisterUses(copy, inputOperands, outputOperands);
    var defs = getRegisterDefs(copy, inputOperands, outputOperands);

    var patterns = generatePatternVariations(instruction,
        labelledMachineInstructions,
        copy,
        inputOperands,
        outputOperands,
        Collections.emptyList(),
        abi);

    return Optional.of(new LlvmLoweringRecord(
        copy,
        inputOperands,
        outputOperands,
        LlvmLoweringPass.Flags.empty(),
        patterns,
        uses,
        defs
    ));
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
    inputOperands.stream().filter(x -> x instanceof TableGenInstructionRegisterFileOperand)
        .findFirst()
        .ifPresent((uncastInputRegister) -> {
          var inputRegister = (TableGenInstructionRegisterFileOperand) uncastInputRegister;
          var selector = new Graph("selector");
          var ref = (ReadRegFileNode) inputRegister.reference().copy();
          var address = (FieldRefNode) ref.address().copy();
          selector.addWithInputs(new LlvmTargetCallSD(new NodeList<>(new LlvmReadRegFileNode(
              inputRegister.registerFile(), address, inputRegister.formatField().type(),
              ref.staticCounterAccess()
          )),
              Type.dummy()));

          var database = new Database(supportedInstructions);
          var jalr =
              database.run(
                      new Query.Builder().machineInstructionLabel(MachineInstructionLabel.JALR).build())
                  .firstMachineInstruction();
          var machine = new Graph("machine");
          var constant = new Constant.Str("0");
          machine.addWithInputs(new LcbMachineInstructionNode(
              new NodeList<>(new ConstantNode(new Constant.Str(abi.returnAddress().render())), ref,
                  new ConstantNode(constant)), jalr));
          var expansion =
              new TableGenPseudoInstExpansionPattern("PseudoCALLIndirect",
                  selector,
                  machine,
                  true,
                  List.of(
                      new TableGenInstructionRegisterFileOperand(
                          ParameterIdentity.from(ref, ref.address()),
                          ref,
                          address.formatField())
                  ), Collections.emptyList());

          result.add(expansion);
        });

    return result;
  }
}
