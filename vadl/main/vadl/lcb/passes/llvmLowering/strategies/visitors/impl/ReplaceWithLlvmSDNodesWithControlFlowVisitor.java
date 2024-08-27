package vadl.lcb.passes.llvmLowering.strategies.visitors.impl;

import java.util.Objects;
import java.util.Set;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmSetccSD;
import vadl.types.BuiltInTable;
import vadl.viam.Register;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Replaces VIAM nodes with LLVM nodes which have more
 * information for the lowering. But this visitor allows if-conditions.
 */
public class ReplaceWithLlvmSDNodesWithControlFlowVisitor
    extends ReplaceWithLlvmSDNodesVisitor {

  @Override
  public void visit(WriteRegNode writeRegNode) {
    if (writeRegNode.value() instanceof LlvmBrCcSD) {
      // already lowered, so skip
      return;
    }

    visit(writeRegNode.value());

    if (writeRegNode.register() instanceof Register.Counter) {
      if (writeRegNode.value() instanceof BuiltInCall builtin && Set.of(
          BuiltInTable.ADD,
          BuiltInTable.ADDS,
          BuiltInTable.SUB
      ).contains(builtin.builtIn())) {
        // We need to four parameters.
        // 1. the conditional code (SETEQ, ...)
        // 2. the first operand of the comparison
        // 3. the second operand of the comparison
        // 4. the immediate offset

        // idea: it would be good to have a link from the side effect to if-node.
        var conditional = getConditional(Objects.requireNonNull(writeRegNode.graph()));
        var condCond = LlvmCondCode.from(conditional.builtIn());
        if (condCond == null) {
          throw new ViamError("CondCode must be not null");
        }

        var first = conditional.arguments().get(0);
        var second = conditional.arguments().get(1);
        var immOffset =
            builtin.arguments().stream().filter(x -> x instanceof LlvmFieldAccessRefNode)
                .findFirst();

        if (immOffset.isEmpty()) {
          throw new ViamError("Immediate Offset is missing");
        }

        writeRegNode.value().replaceAndDelete(new LlvmBrCcSD(
            condCond,
            first,
            second,
            immOffset.get()
        ));
      }
    }
  }


  @Override
  public void visit(IfNode ifNode) {

  }

  @Override
  public void visit(SideEffectNode node) {
    node.accept(this);
  }

  @Override
  public void visit(ExpressionNode node) {
    node.accept(this);
  }

  @Override
  public void visit(Node node) {
    node.accept(this);
  }

  private LlvmSetccSD getConditional(Graph behavior) {
    var builtIn = behavior.getNodes(LlvmSetccSD.class)
        .filter(
            x -> Set.of(BuiltInTable.EQU, BuiltInTable.NEQ, BuiltInTable.SLTH, BuiltInTable.ULTH,
                    BuiltInTable.SGEQ, BuiltInTable.UGEQ, BuiltInTable.SLEQ, BuiltInTable.ULEQ)
                .contains(x.builtIn()))
        .findFirst();

    if (builtIn.isEmpty()) {
      throw new ViamError(
          "Visitor wrongly used. Are you sure this is a conditional branch instruction?");
    }

    return builtIn.get();
  }
}
