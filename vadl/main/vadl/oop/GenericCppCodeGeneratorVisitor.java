package vadl.oop;

import static vadl.oop.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import java.util.Objects;
import vadl.types.Type;
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
 * Generic cpp code generator which can be used to emit cpp code for the lcb and qemu.
 */
public abstract class GenericCppCodeGeneratorVisitor implements GraphNodeVisitor {
  protected final StringWriter writer;

  public GenericCppCodeGeneratorVisitor(StringWriter writer) {
    this.writer = writer;
  }

  protected String generateBitmask(int size) {
    return String.format("((1U << %d) - 1)", size);
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
    if (node.arguments().size() == 1) {
      node.ensure(node.builtIn().operator() != null, "Operator must not be null");
      writer.write(Objects.requireNonNull(node.builtIn().operator()));
      visit(node.arguments().get(0));
    } else if (node.arguments().size() > 1) {
      for (int i = 0; i < node.arguments().size(); i++) {
        writer.write("(");
        visit(node.arguments().get(i));
        writer.write(")");

        // The last argument should not emit an operand.
        if (i < node.arguments().size() - 1) {
          writer.write(" " + node.builtIn().operator() + " ");
        }
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
    if (typeCastNode.castType() == Type.bool()) {
      // Integer downcasts truncated the higher bits
      // but, boolean downcasts (v != 0)
      writer.write("((" + getCppTypeNameByVadlType(typeCastNode.castType()) + ") ");
      visit(typeCastNode.value());
      writer.write(" & 0x1)");
    } else {
      writer.write("((" + getCppTypeNameByVadlType(typeCastNode.castType()) + ") ");
      visit(typeCastNode.value());
      writer.write(")");
    }
  }

  @Override
  public void visit(SliceNode sliceNode) {
    sliceNode.ensure(sliceNode.bitSlice().isContinuous(), "We only support continuous slices");
    writer.write("(((");
    visit(sliceNode.value());
    writer.write(")");
    sliceNode.bitSlice().parts().forEach(part -> {
      if (part.lsb() > 0) {
        writer.write(
            " & " + generateBitmask(part.msb() + 1) + " & ~((1 << " + part.lsb() + ") - 1))");
      } else {
        writer.write(
            " & " + generateBitmask(part.msb() + 1) + ")");
      }

      // First, we cleared the bits
      // Now, extract the bits by shifting the lowest bits.
      writer.write(" >> " + part.lsb() + ")");
    });
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
