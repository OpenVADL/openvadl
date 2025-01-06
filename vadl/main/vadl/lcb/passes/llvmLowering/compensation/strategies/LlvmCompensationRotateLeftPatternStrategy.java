package vadl.lcb.passes.llvmLowering.compensation.strategies;

import static vadl.viam.ViamError.ensurePresent;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.isaMatching.database.BehaviorQuery;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmRotlSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Strategy for generating rotate-left.
 */
public class LlvmCompensationRotateLeftPatternStrategy implements LlvmCompensationPatternStrategy {
  private static final Query orQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.OR)
      .build();
  private static final Query sllQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.SLL)
      .withBehavior(new BehaviorQuery(
          BuiltInCall.class,
          builtInCall -> builtInCall.usages()
              .noneMatch(x -> x instanceof SignExtendNode || x instanceof ZeroExtendNode)))
      .build();
  private static final Query srlQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.SRL)
      .withBehavior(new BehaviorQuery(
          BuiltInCall.class,
          builtInCall -> builtInCall.usages()
              .noneMatch(x -> x instanceof SignExtendNode || x instanceof ZeroExtendNode)))
      .build();
  private static final Query subQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.SUB)
      .withBehavior(new BehaviorQuery(
          BuiltInCall.class,
          builtInCall -> builtInCall.usages()
              .noneMatch(x -> x instanceof SignExtendNode || x instanceof ZeroExtendNode)))
      .build();

  private static final Query liQuery = new Query.Builder()
      .pseudoInstructionLabel(PseudoInstructionLabel.LI)
      .build();

  @Override
  public boolean isApplicable(Database database) {
    var hasRotl = !database.run(new Query.Builder()
        .machineInstructionLabel(MachineInstructionLabel.ROTL)
        .build()).machineInstructions().isEmpty();

    if (!hasRotl) {
      var exec = database.run(new Query.Builder()
          .machineInstructionLabel(MachineInstructionLabel.OR)
          .or(sllQuery)
          .or(srlQuery)
          .or(subQuery)
          .or(liQuery)
          .build()
      );
      return !exec.pseudoInstructions().isEmpty() && !exec.machineInstructions().isEmpty();
    }

    return false;
  }

  @Override
  public Collection<TableGenSelectionWithOutputPattern> lower(Database database,
                                                              Specification viam) {
    /*
    def : Pat< ( rotl X:$rs1, X:$rs2 ),
           ( OR (SLL X:$rs1, X:$rs2), (SRL X:$rs1, (SUB (LI (i32 32)), X:$rs2))) >;
     */
    var or = database.run(orQuery)
        .firstMachineInstruction();
    var sll = database.run(sllQuery)
        .firstMachineInstruction();
    var srl = database.run(srlQuery)
        .firstMachineInstruction();
    var sub = database.run(subQuery)
        .firstMachineInstruction();
    var li = database.run(liQuery)
        .firstPseudoInstruction();

    var operands = or.behavior().getNodes(ReadRegFileNode.class).toList();
    var rotlGraph = setupSelector(operands, or);
    var rotlMachineGraph = setupMachine(operands, or, sub, sll, srl, li);

    return List.of(new TableGenSelectionWithOutputPattern(rotlGraph, rotlMachineGraph));
  }

  private Graph setupSelector(List<ReadRegFileNode> operands, Instruction or) {
    var liftedOperands = liftOperands(operands);

    var rotlGraph = new Graph("rotl.selector");
    var selector = new LlvmRotlSD(new NodeList<>(liftedOperands), or.format().type());
    rotlGraph.addWithInputs(selector);
    return rotlGraph;
  }

  private Graph setupMachine(List<ReadRegFileNode> operands,
                             Instruction or,
                             Instruction sub,
                             Instruction sll,
                             Instruction srl,
                             PseudoInstruction li) {
    var liftedOperands = liftOperands(operands);
    var rotlMachineGraph = new Graph("rotl.machine");
    var origType = liftedOperands.get(0).type();
    var operandType = ensurePresent(ValueType.from(origType),
        () -> Diagnostic.error("Cannot construct llvm type from type.",
            liftedOperands.get(0).sourceLocation()));
    var machineLi =
        new LcbPseudoInstructionNode(new NodeList<>(new LcbMachineInstructionValueNode(operandType,
            Constant.Value.of(operandType.getBitwidth(), origType))), li);
    var machineSub =
        new LcbMachineInstructionNode(new NodeList<>(machineLi, liftedOperands.get(1)), sub);
    var machineSll = new LcbMachineInstructionNode(new NodeList<>(liftedOperands), sll);
    var machineSrl =
        new LcbMachineInstructionNode(new NodeList<>(liftedOperands.get(0), machineSub), srl);
    var machineInstructionNode = new LcbMachineInstructionNode(
        new NodeList<>(machineSll, machineSrl), or);
    rotlMachineGraph.addWithInputs(machineInstructionNode);
    return rotlMachineGraph;
  }

  private static @Nonnull List<LlvmReadRegFileNode> liftOperands(
      List<ReadRegFileNode> operands) {
    return operands.stream()
        .map(readRegFileNode -> new LlvmReadRegFileNode(readRegFileNode.registerFile(),
            (ExpressionNode) readRegFileNode.address().copy(),
            readRegFileNode.type(),
            readRegFileNode.staticCounterAccess())).toList();
  }
}
