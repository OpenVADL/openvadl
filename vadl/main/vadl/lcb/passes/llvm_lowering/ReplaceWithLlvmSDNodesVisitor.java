package vadl.lcb.passes.llvm_lowering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.LcbGraphNodeVisitor;
import vadl.lcb.passes.llvm_lowering.model.LlvmAddSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmAndSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmMulSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmOrSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmSDivSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmSRemSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmShlSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmShrSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmSraSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmSubSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmUDivSD;
import vadl.lcb.passes.llvm_lowering.model.LlvmURemSD;
import vadl.pass.PassManager;
import vadl.types.BuiltInTable;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
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
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

public class ReplaceWithLlvmSDNodesVisitor implements LcbGraphNodeVisitor {

  private static final Logger logger = LoggerFactory.getLogger(ReplaceWithLlvmSDNodesVisitor.class);
  private boolean patternLowerable = true;

  /**
   * Returns whether the {@link ReplaceWithLlvmSDNodesVisitor} encountered nodes which are not
   * lowerable.
   *
   * @return {@code true} when the graph is ok.
   */
  public boolean isPatternLowerable() {
    return this.patternLowerable;
  }

  @Override
  public void visit(ConstantNode node) {

  }

  @Override
  public void visit(BuiltInCall node) {
    if (node.builtIn() == BuiltInTable.ADD || node.builtIn() == BuiltInTable.ADDS) {
      node.replaceAndDelete(new LlvmAddSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SUB) {
      node.replaceAndDelete(new LlvmSubSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MUL || node.builtIn() == BuiltInTable.MULS) {
      node.replaceAndDelete(new LlvmMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.DIV_SS || node.builtIn() == BuiltInTable.DIVS_SS) {
      node.replaceAndDelete(new LlvmSDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.DIV_UU || node.builtIn() == BuiltInTable.DIVS_UU) {
      node.replaceAndDelete(new LlvmUDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MOD_SS || node.builtIn() == BuiltInTable.MODS_SS) {
      node.replaceAndDelete(new LlvmSRemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MOD_UU || node.builtIn() == BuiltInTable.MODS_UU) {
      node.replaceAndDelete(new LlvmURemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.AND || node.builtIn() == BuiltInTable.ANDS) {
      node.replaceAndDelete(new LlvmAndSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.OR || node.builtIn() == BuiltInTable.ORS) {
      node.replaceAndDelete(new LlvmOrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSL || node.builtIn() == BuiltInTable.LSLS) {
      node.replaceAndDelete(new LlvmShlSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSR || node.builtIn() == BuiltInTable.LSRS) {
      node.replaceAndDelete(new LlvmShrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.ASR || node.builtIn() == BuiltInTable.ASRS) {
      node.replaceAndDelete(new LlvmSraSD(node.arguments(), node.type()));
    }

    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(TypeCastNode typeCastNode) {
    throw new RuntimeException("Must not exist");
  }

  @Override
  public void visit(SliceNode sliceNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SelectNode selectNode) {
    logger.atWarn().log("Conditionals are in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(LetNode letNode) {
    letNode.replaceAndDelete(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    logger.warn("Function calls are in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
    // it is ok
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReturnNode returnNode) {
    logger.warn("Return node is in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(EndNode endNode) {
    // it is ok
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(IfNode ifNode) {
    logger.warn("Conditionals are in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }
}
