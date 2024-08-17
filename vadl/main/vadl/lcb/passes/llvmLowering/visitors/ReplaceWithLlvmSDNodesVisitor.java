package vadl.lcb.passes.llvmLowering.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.model.LlvmAddSD;
import vadl.lcb.passes.llvmLowering.model.LlvmAndSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmMulSD;
import vadl.lcb.passes.llvmLowering.model.LlvmOrSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSDivSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSMulSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSRemSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSetccSD;
import vadl.lcb.passes.llvmLowering.model.LlvmShlSD;
import vadl.lcb.passes.llvmLowering.model.LlvmShrSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSraSD;
import vadl.lcb.passes.llvmLowering.model.LlvmSubSD;
import vadl.lcb.passes.llvmLowering.model.LlvmUDivSD;
import vadl.lcb.passes.llvmLowering.model.LlvmUMulSD;
import vadl.lcb.passes.llvmLowering.model.LlvmURemSD;
import vadl.lcb.passes.llvmLowering.model.LlvmXorSD;
import vadl.lcb.visitors.LcbGraphNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
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
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Replaces VIAM nodes with LLVM nodes which have more
 * information for the lowering.
 */
public class ReplaceWithLlvmSDNodesVisitor
    implements LcbGraphNodeVisitor, TableGenPatternLowerable {

  private static final Logger logger = LoggerFactory.getLogger(ReplaceWithLlvmSDNodesVisitor.class);
  private boolean patternLowerable = true;

  @Override
  public boolean isPatternLowerable() {
    return this.patternLowerable;
  }

  @Override
  public void visit(Node node) {
    node.accept(this);
  }

  @Override
  public void visit(ConstantNode node) {

  }

  @Override
  public void visit(BuiltInCall node) {
    if (node instanceof LlvmNodeLowerable) {
      for (var arg : node.arguments()) {
        visit(arg);
      }

      return;
    }

    if (node.builtIn() == BuiltInTable.ADD || node.builtIn() == BuiltInTable.ADDS) {
      node.replaceAndDelete(new LlvmAddSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SUB) {
      node.replaceAndDelete(new LlvmSubSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.MUL || node.builtIn() == BuiltInTable.MULS) {
      node.replaceAndDelete(new LlvmMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SMULL || node.builtIn() == BuiltInTable.SMULLS) {
      node.replaceAndDelete(new LlvmSMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UMULL || node.builtIn() == BuiltInTable.UMULLS) {
      node.replaceAndDelete(new LlvmUMulSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SDIV || node.builtIn() == BuiltInTable.SDIVS) {
      node.replaceAndDelete(new LlvmSDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UDIV || node.builtIn() == BuiltInTable.UDIVS) {
      node.replaceAndDelete(new LlvmUDivSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.SMOD || node.builtIn() == BuiltInTable.SMODS) {
      node.replaceAndDelete(new LlvmSRemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.UMOD || node.builtIn() == BuiltInTable.UMODS) {
      node.replaceAndDelete(new LlvmURemSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.AND || node.builtIn() == BuiltInTable.ANDS) {
      node.replaceAndDelete(new LlvmAndSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.OR || node.builtIn() == BuiltInTable.ORS) {
      node.replaceAndDelete(new LlvmOrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.XOR || node.builtIn() == BuiltInTable.XORS) {
      node.replaceAndDelete(new LlvmXorSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSL || node.builtIn() == BuiltInTable.LSLS) {
      node.replaceAndDelete(new LlvmShlSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.LSR || node.builtIn() == BuiltInTable.LSRS) {
      node.replaceAndDelete(new LlvmShrSD(node.arguments(), node.type()));
    } else if (node.builtIn() == BuiltInTable.ASR || node.builtIn() == BuiltInTable.ASRS) {
      node.replaceAndDelete(new LlvmSraSD(node.arguments(), node.type()));
    } else if (LlvmSetccSD.supported.contains(node.builtIn())) {
      node.replaceAndDelete(new LlvmSetccSD(node.builtIn(), node.arguments(), node.type()));
    } else {
      throw new RuntimeException("not implemented: " + node.builtIn());
    }

    for (var arg : node.arguments()) {
      visit(arg);
    }
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
    logger.atWarn().log("Conditionals are in the instruction's behavior is not lowerable");
    patternLowerable = false;
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
    letNode.replaceAndDelete(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
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
    if (!(fieldAccessRefNode instanceof LlvmFieldAccessRefNode)) {
      fieldAccessRefNode.replaceAndDelete(
          new LlvmFieldAccessRefNode(fieldAccessRefNode.fieldAccess(),
              fieldAccessRefNode.type()));
    }
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
    //throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(IfNode ifNode) {
    logger.warn("Conditionals are in the instruction's behavior is not lowerable");
    patternLowerable = false;
  }

  @Override
  public void visit(ZeroExtendNode node) {
    // Remove all nodes
    for (var usage : node.usages().toList()) {
      usage.replaceInput(node, node.value());
    }
    visit(node.value());
  }

  @Override
  public void visit(SignExtendNode node) {
    // Remove all nodes
    for (var usage : node.usages().toList()) {
      usage.replaceInput(node, node.value());
    }
    visit(node.value());
  }

  @Override
  public void visit(TruncateNode node) {
    // Remove all nodes
    for (var usage : node.usages().toList()) {
      usage.replaceInput(node, node.value());
    }
    visit(node.value());
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
