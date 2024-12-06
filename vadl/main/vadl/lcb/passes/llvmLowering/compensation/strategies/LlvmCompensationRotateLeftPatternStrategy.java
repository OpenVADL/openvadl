package vadl.lcb.passes.llvmLowering.compensation.strategies;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.isaMatching.database.BehaviorQuery;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.types.BuiltInTable;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Strategy for generating rotate-left.
 */
public class LlvmCompensationRotateLeftPatternStrategy implements LlvmCompensationPatternStrategy {
  private static final Predicate<Node> pure =
      builtInCall -> builtInCall.usages()
          .noneMatch(x -> x instanceof SignExtendNode || x instanceof ZeroExtendNode);

  private static final Query orQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.OR)
      .build();
  private static final Query sllQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.SLL)
      .withBehavior(new BehaviorQuery(
          BuiltInCall.class,
          pure))
      .build();
  private static final Query srlQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.SRL)
      .withBehavior(new BehaviorQuery(
          BuiltInCall.class,
          pure))
      .build();
  private static final Query subQuery = new Query.Builder()
      .machineInstructionLabel(MachineInstructionLabel.SUB)
      .withBehavior(new BehaviorQuery(
          BuiltInCall.class,
          pure))
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
  public Collection<? extends TableGenPattern> lower(Database database, Specification viam) {
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


    return Collections.emptyList();
  }
}
