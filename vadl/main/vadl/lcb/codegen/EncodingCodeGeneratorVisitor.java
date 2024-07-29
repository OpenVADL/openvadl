package vadl.lcb.codegen;

import static vadl.oop.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import vadl.oop.OopGraphNodeVisitor;
import vadl.oop.passes.type_normalization.UpcastedTypeCastNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.viam.ViamError;
import vadl.viam.Constant;
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

/**
 * The {@link GraphNodeVisitor} for the {@link EncodingCodeGenerator}.
 * The tasks of this class is to generate the Cpp code for LLVM which
 * is called by tablegen to encode an immediate.
 */
public class EncodingCodeGeneratorVisitor implements OopGraphNodeVisitor {
  private final StringWriter writer;

  public EncodingCodeGeneratorVisitor(StringWriter writer) {
    this.writer = writer;
  }

  private String generateBitmask(int size) {
    return String.format("(1U << %d) - 1", size);
  }

  @Override
  public void visit(ConstantNode node) {
    var constant = node.constant();
    constant.ensure(constant instanceof Constant.Value || constant instanceof Constant.Str,
        "Only value and string constant are currently supported for CPP emitting");

    if (constant instanceof Constant.Value) {
      writer.write(((Constant.Value) constant).integer().toString(10));
    } else {
      writer.write(((Constant.Str) constant).value());
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
    writer.write("(" + getCppTypeNameByVadlType(typeCastNode.castType()) + ") ");
    visit(typeCastNode.value());
  }

  @Override
  public void visit(UpcastedTypeCastNode upcastedTypeCastNode) {
    var castType = upcastedTypeCastNode.castType();
    var originalType = upcastedTypeCastNode.originalType();
    writer.write("((" + getCppTypeNameByVadlType(castType) + ") ");
    visit(upcastedTypeCastNode.value());

    upcastedTypeCastNode.ensure(originalType instanceof BitsType
        || originalType instanceof BoolType, "Type must be bits or bool");

    if (originalType instanceof BitsType cast) {
      writer.write(
          ") & " + generateBitmask(cast.bitWidth()));
    } else if (upcastedTypeCastNode.originalType() instanceof BoolType) {
      writer.write(") & 1");
    }
  }

  @Override
  public void visit(SliceNode sliceNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(SelectNode selectNode) {
    throw new RuntimeException("not implemented");
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
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    writer.write(funcParamNode.parameter().name());
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    var name = funcCallNode.function().name();

    writer.write(name + "(");

    for (int i = 0; i < funcCallNode.arguments().size(); i++) {
      visit(funcCallNode.arguments().get(i));
      if (i < funcCallNode.arguments().size() - 1) {
        writer.write(",");
      }
    }

    writer.write(")");
  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(InstrEndNode instrEndNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ReturnNode returnNode) {
    writer.write("return ");
    visit(returnNode.value);
  }

  @Override
  public void visit(EndNode endNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(IfNode ifNode) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    // We have to dispatch here because
    // we would need a cast. However,
    // we do not want to create explicit casts by hand.
    expressionNode.accept(this);
  }

}
