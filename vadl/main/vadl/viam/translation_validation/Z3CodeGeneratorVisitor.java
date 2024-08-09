package vadl.viam.translation_validation;

import java.io.StringWriter;
import java.util.Map;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;
import vadl.types.UIntType;
import vadl.viam.Constant;
import vadl.viam.Identifier;
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
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

public class Z3CodeGeneratorVisitor implements GraphNodeVisitor {
  private final StringWriter writer = new StringWriter();

  // VADL can have multiple memories. This symbol table keeps track of different
  // kinds of memory.
  private final Map<Identifier, String> memoryMap;

  public Z3CodeGeneratorVisitor(Map<Identifier, String> memoryMap) {
    this.memoryMap = memoryMap;
  }

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Value) {
      writer.write(String.format("BitVecVal(%d, %d)",
          ((Constant.Value) node.constant()).integer(),
          ((BitsType) node.constant().type()).bitWidth()));
    } else {
      throw new ViamError("not implemented");
    }
  }


  @Override
  public void visit(BuiltInCall node) {
    if (node.builtIn() == BuiltInTable.NEG) {
      writer.write("(");
      writer.write("-1 * ");
      visit(node.arguments().get(0));
      writer.write(")");
    } else {
      node.ensure(node.arguments().size() > 1,
          "This method only works for more than 1 arguments");
      for (int i = 0; i < node.arguments().size(); i++) {
        visit(node.arguments().get(i));

        // The last argument should not emit an operand.
        if (i < node.arguments().size() - 1) {
          writer.write(" " + node.builtIn().operator() + " ");
        }
      }
    }
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {
    visit(writeRegNode.value());
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
    visit(writeRegFileNode.value());
  }

  @Override
  public void visit(WriteMemNode writeMemNode) {
    // TODO: What about the address?
    visit(writeMemNode.value());
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
    } else {
      visit(typeCastNode.value());
    }
  }

  @Override
  public void visit(SliceNode sliceNode) {
    writer.write("Extract("
        + sliceNode.bitSlice().msb() + ", "
        + sliceNode.bitSlice().lsb() + ", ");
    visit(sliceNode.value());
    writer.write(")");
  }

  @Override
  public void visit(SelectNode selectNode) {
    writer.write("If(");
    visit(selectNode.condition());
    writer.write(",");
    visit(selectNode.trueCase());
    writer.write(",");
    visit(selectNode.falseCase());
    writer.write(")");
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    writer.write(readRegNode.register().identifier.simpleName());
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    writer.write(readRegFileNode.registerFile().identifier.simpleName());
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    writer.write(memoryMap.get(readMemNode.memory().identifier));
  }

  @Override
  public void visit(LetNode letNode) {
    visit(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    writer.write(funcParamNode.parameter().identifier.simpleName());
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    writer.write(funcCallNode.function().identifier.simpleName());
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    writer.write(fieldRefNode.formatField().simpleName());
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    writer.write(fieldAccessRefNode.fieldAccess().simpleName());
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

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }
}
