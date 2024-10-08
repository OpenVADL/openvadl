package vadl.cppCodeGen;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import java.io.StringWriter;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.cppCodeGen.model.CppUpdateBitRangeNode;
import vadl.cppCodeGen.passes.typeNormalization.CppSignExtendNode;
import vadl.cppCodeGen.passes.typeNormalization.CppTruncateNode;
import vadl.cppCodeGen.passes.typeNormalization.CppZeroExtendNode;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
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
 * Generic cpp code generator which can be used to emit cpp code for the lcb and qemu.
 */
public class GenericCppCodeGeneratorVisitor
    implements GraphNodeVisitor, CppCodeGenGraphNodeVisitor {
  protected final StringWriter writer;

  public GenericCppCodeGeneratorVisitor(StringWriter writer) {
    this.writer = writer;
  }

  protected String generateBitmask(int size) {
    return String.format("((1UL << %d) - 1)", size);
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
          writer.write(" " + BuiltInTranslator.map(node.builtIn()) + " ");
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
  public void visit(SliceNode sliceNode) {
    var parts = sliceNode.bitSlice().parts().toList();
    writer.write("(");

    int acc = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      if (i != parts.size() - 1) {
        writer.write(" | ");
      }

      var part = parts.get(i);
      var bitWidth = ((BitsType) sliceNode.value().type()).bitWidth();
      if (part.isIndex()) {
        writer.write(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        visit(sliceNode.value()); // same expression
        writer.write(String.format(")) << %d", acc));
      } else {
        writer.write(
            String.format("project_range<%d, %d>(std::bitset<%d>(", part.lsb(), part.msb(),
                bitWidth));
        visit(sliceNode.value()); // same expression
        writer.write(String.format(")) << %d", acc));
      }

      acc += part.msb() - part.lsb() + 1;
    }
    writer.write(").to_ulong()");
  }

  @Override
  public void visit(CppUpdateBitRangeNode cppUpdateBitRangeNode) {
    var bitWidth = ((BitsType) cppUpdateBitRangeNode.type()).bitWidth();
    writer.write("set_bits(");

    // Inst
    writer.write(String.format("std::bitset<%d>(", bitWidth));
    visit(cppUpdateBitRangeNode.value);
    writer.write("), ");

    // New value
    writer.write(String.format("std::bitset<%d>(", bitWidth));
    visit(cppUpdateBitRangeNode.patch);
    writer.write(")");

    // Parts
    writer.write(", std::vector<int> { ");
    writer.write(cppUpdateBitRangeNode.field.bitSlice()
        .stream()
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(", ")));
    writer.write(" } ");

    writer.write(").to_ulong()");
  }

  @Override
  public void visit(SelectNode selectNode) {
    visit(selectNode.condition());
    writer.write("? ");
    visit(selectNode.trueCase());
    writer.write(":");
    visit(selectNode.falseCase());
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
    visit(letNode.expression());
  }

  @Override
  public void visit(FuncParamNode funcParamNode) {
    writer.write(funcParamNode.parameter().simpleName());
  }

  @Override
  public void visit(FuncCallNode funcCallNode) {
    var name = funcCallNode.function().simpleName();

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
    visit(returnNode.value());
  }

  @Override
  public void visit(BranchEndNode branchEndNode) {
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

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  @Override
  public void visit(ZeroExtendNode node) {
    writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(SignExtendNode node) {
    writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(TruncateNode node) {
    if (node.type() instanceof BoolType) {
      writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
      visit(node.value());
      writer.write(" & 0x1)");
    } else {
      writer.write("((" + getCppTypeNameByVadlType(node.type()) + ") ");
      visit(node.value());
      writer.write(")");
    }
  }

  @Override
  public void visit(CppSignExtendNode node) {
    visit((SignExtendNode) node);

    if (node.originalType() instanceof BitsType bitsType) {
      writer.write(" & " + generateBitmask(bitsType.bitWidth()));
    }
  }

  @Override
  public void visit(CppZeroExtendNode node) {
    visit((ZeroExtendNode) node);

    if (node.originalType() instanceof BitsType bitsType) {
      writer.write(" & " + generateBitmask(bitsType.bitWidth()));
    }
  }

  @Override
  public void visit(CppTruncateNode node) {
    visit((TruncateNode) node);
  }
}
