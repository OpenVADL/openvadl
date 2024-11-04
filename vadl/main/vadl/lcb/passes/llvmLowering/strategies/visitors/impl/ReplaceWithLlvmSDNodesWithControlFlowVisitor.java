package vadl.lcb.passes.llvmLowering.strategies.visitors.impl;

import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Replaces VIAM nodes with LLVM nodes which have more
 * information for the lowering. But this visitor allows if-conditions.
 */
public class ReplaceWithLlvmSDNodesWithControlFlowVisitor
    extends ReplaceWithLlvmSDNodesVisitor {

  /**
   * Constructor.
   *
   * @param architectureType is the type for which tablegen types should be upcasted to.
   *                         On 32 Bit architectures should all immediates upcasted to 32 Bit.
   */
  public ReplaceWithLlvmSDNodesWithControlFlowVisitor(ValueType architectureType) {
    super(architectureType);
  }

  @Override
  public void visit(SideEffectNode node) {
    node.accept(this);
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    var originalType = fieldAccessRefNode.type();

    fieldAccessRefNode.replaceAndDelete(new LlvmBasicBlockSD(fieldAccessRefNode.fieldAccess(),
        originalType,
        architectureType));
  }

  @Override
  public void visit(ExpressionNode node) {
    node.accept(this);
  }

  @Override
  public void visit(Node node) {
    node.accept(this);
  }


}
