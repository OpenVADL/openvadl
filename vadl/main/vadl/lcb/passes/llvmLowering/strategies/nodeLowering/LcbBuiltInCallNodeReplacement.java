package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import static vadl.viam.ViamError.ensure;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmAndSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmMulSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmOrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSDivSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSRemSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShlSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmShrSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSraSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSubSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUDivSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmURemSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmUnlowerableSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmXorSD;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbBuiltInCallNodeReplacement
    implements GraphVisitor.NodeApplier<BuiltInCall, BuiltInCall> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;
  // Builtins which do not need to be handled here because they are handled elsewhere.
  private final Set<BuiltInTable.BuiltIn> exceptions =
      Set.of(BuiltInTable.SMULL, BuiltInTable.SMULLS, BuiltInTable.UMULL, BuiltInTable.UMULLS);

  public LcbBuiltInCallNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public BuiltInCall visit(BuiltInCall node) {
    for (var arg : node.arguments()) {
      visitApplicable(arg);
    }

    if (node.builtIn() == BuiltInTable.ADD || node.builtIn() == BuiltInTable.ADDS) {
      return node.replaceAndDelete(new LlvmAddSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SUB) {
      return node.replaceAndDelete(new LlvmSubSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MUL || node.builtIn() == BuiltInTable.MULS) {
      return node.replaceAndDelete(new LlvmMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SDIV || node.builtIn() == BuiltInTable.SDIVS) {
      return node.replaceAndDelete(new LlvmSDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UDIV || node.builtIn() == BuiltInTable.UDIVS) {
      return node.replaceAndDelete(new LlvmUDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SMOD || node.builtIn() == BuiltInTable.SMODS) {
      return node.replaceAndDelete(new LlvmSRemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UMOD || node.builtIn() == BuiltInTable.UMODS) {
      return node.replaceAndDelete(new LlvmURemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.AND || node.builtIn() == BuiltInTable.ANDS) {
      return node.replaceAndDelete(new LlvmAndSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.OR || node.builtIn() == BuiltInTable.ORS) {
      return node.replaceAndDelete(new LlvmOrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.XOR || node.builtIn() == BuiltInTable.XORS) {
      return node.replaceAndDelete(new LlvmXorSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSL || node.builtIn() == BuiltInTable.LSLS) {
      return node.replaceAndDelete(new LlvmShlSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSR || node.builtIn() == BuiltInTable.LSRS) {
      return node.replaceAndDelete(new LlvmShrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.ASR || node.builtIn() == BuiltInTable.ASRS) {
      return node.replaceAndDelete(new LlvmSraSD(node.arguments(), node.type()));
    } else if (LlvmSetccSD.supported.contains(node.builtIn())) {
      var replaced = node.replaceAndDelete(
          new LlvmSetccSD(node.builtIn(), node.arguments(), node.type()));
      //def : Pat< ( setcc X:$rs1, 0, SETEQ ),
      //           ( SLTIU X:$rs1, 1 ) >;
      // By adding it as argument, we get the printing of "SETEQ" for free.
      var newArg = new ConstantNode(new Constant.Str(replaced.llvmCondCode().name()));
      ensure(replaced.graph() != null, "graph must exist");
      replaced.arguments().add(replaced.graph().addWithInputs(newArg));
      return replaced;
    } else if ((node.builtIn() == BuiltInTable.SMULL || node.builtIn() == BuiltInTable.SMULLS)
        && node.type() instanceof BitsType bitsType) {
      var trunc = bitsType.bitWidth() / 2;

      // Only replace when parent is a truncate node to the half bit width.
      var truncNode = node.usages().findFirst().filter(x -> x instanceof TruncateNode y
          && y.type().bitWidth() == trunc);
      if (truncNode.isPresent()) {
        return truncNode.get()
            .replaceAndDelete(
                new LlvmMulSD(node.arguments(), ((TruncateNode) truncNode.get()).type()));
      }
    }

    return node.replaceAndDelete(new LlvmUnlowerableSD(node.arguments(), node.type()));
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof BuiltInCall bc && !exceptions.contains(bc.builtIn());
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
