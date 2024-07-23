package vadl.lcb.codegen;

import java.io.StringWriter;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.types.UIntType;
import vadl.viam.Constant;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
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

public class Z3EncodingCodeGeneratorVisitor implements GraphNodeVisitor {

  private final StringWriter writer = new StringWriter();
  private final String symbolName;

  // from z3 import *
  // x = BitVec('x', 20) # field
  // f_x = ZeroExt(12, x)
  // f_z = Extract(19, 0, f_x)
  // prove (x == f_z)
  //
  // The trick is that f_z references f_x and
  // does all the inverse operations.
  // However, we want to apply for both functions
  // the same visitor.
  // That's why we have 'symbolName' in the constructor.
  // In the case of 'f_x' this is the field
  // In the case of 'f_z' this is the function parameter
  public Z3EncodingCodeGeneratorVisitor(String symbolName) {
    this.symbolName = symbolName;
  }

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Value) {
      writer.write(node.constant().toString());
    } else {
      throw new ViamError("not implemented");
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    for (int i = 0; i < node.arguments().size(); i++) {
      visit(node.arguments().get(i));

      // The last argument should not emit an operand.
      if (i < node.arguments().size() - 1) {
        writer.write(" " + node.builtIn().operator() + " ");
      }
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
  public void visit(TypeCastNode typeCastNode) {
    if (typeCastNode.castType() instanceof UIntType) {
      var width = ((UIntType) typeCastNode.castType()).bitWidth()
          - ((BitsType) typeCastNode.value().type()).bitWidth();
      writer.write("ZeroExt(" + width + ", ");
      visit(typeCastNode.value());
      writer.write(")");
    } else if (typeCastNode.castType() instanceof SIntType) {
      var width = ((SIntType) typeCastNode.castType()).bitWidth()
          - ((BitsType) typeCastNode.value().type()).bitWidth();
      writer.write("SignExt(" + width + ", ");
      visit(typeCastNode.value());
      writer.write(")");
    }
  }

  @Override
  public void visit(SliceNode sliceNode) {
    writer.write("Extract(" +
        sliceNode.bitSlice().msb() + ", "
        + sliceNode.bitSlice().lsb() + ", ");
    visit(sliceNode.value());
    writer.write(")");
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
    writer.write(symbolName);
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {

  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    writer.write(symbolName);
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
    visit(returnNode.value);
  }

  @Override
  public void visit(EndNode endNode) {

  }

  @Override
  public void visit(InstrCallNode instrCallNode) {

  }

  @Override
  public void visit(IfNode ifNode) {

  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }
}
