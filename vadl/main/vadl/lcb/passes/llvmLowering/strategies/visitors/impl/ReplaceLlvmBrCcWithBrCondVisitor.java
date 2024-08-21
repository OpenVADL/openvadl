package vadl.lcb.passes.llvmLowering.strategies.visitors.impl;

import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmCondCode;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.visitors.impl.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

/**
 * Replaces {@link LlvmBrCcSD} with {@link LlvmBrCondSD} for the
 * {@link LlvmLoweringConditionalBranchesStrategyImpl} which creates alternative patterns.
 */
public class ReplaceLlvmBrCcWithBrCondVisitor extends ReplaceWithLlvmSDNodesVisitor
    implements TableGenNodeVisitor {
  private boolean changed = false;

  public boolean isChanged() {
    return changed;
  }

  @Override
  public void visit(LlvmBrCcSD node) {
    /*
    def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
          (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;

    to

    def : Pat<(brcond (i32 (seteq X:$rs1, X:$rs2)), bb:$imm12),
        (BEQ X:$rs1, X:$rs2, RV32I_Btype_ImmediateB_immediateAsLabel:$imm)>;
     */

    // For `brcc` we have Setcc code, so need to see if we have a suitable
    // instruction for that.
    var builtin = LlvmCondCode.from(node.condition());
    var builtinCall =
        new BuiltInCall(builtin, new NodeList<>(node.first(), node.second()), node.first().type());

    // We also extend the result of the condition to i32.
    var typeCast = new LlvmTypeCastSD(builtinCall, Type.signedInt(32));
    var brCond = new LlvmBrCondSD(typeCast, node.immOffset());
    node.replaceAndDelete(brCond);
    changed = true;

    visit(brCond.condition());
    visit(brCond.immOffset());
  }

  @Override
  public void visit(LlvmFieldAccessRefNode llvmFieldAccessRefNode) {

  }

  @Override
  public void visit(LlvmBrCondSD node) {

  }

  @Override
  public void visit(LlvmTypeCastSD node) {
    visit(node.value());
  }

  @Override
  public void visit(ConstantNode node) {

  }

  @Override
  public void visit(BuiltInCall node) {
    super.visit(node);
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {

  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {

  }

  @Override
  public void visit(WriteMemNode writeMemNode) {

  }

  @Override
  public void visit(SliceNode sliceNode) {

  }

  @Override
  public void visit(SelectNode selectNode) {

  }

  @Override
  public void visit(ReadRegNode readRegNode) {

  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {

  }

  @Override
  public void visit(ReadMemNode readMemNode) {

  }

  @Override
  public void visit(LetNode letNode) {

  }

  @Override
  public void visit(FuncParamNode funcParamNode) {

  }

  @Override
  public void visit(FuncCallNode funcCallNode) {

  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {

  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {

  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {

  }

  @Override
  public void visit(InstrEndNode instrEndNode) {

  }

  @Override
  public void visit(ReturnNode returnNode) {

  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }
}
