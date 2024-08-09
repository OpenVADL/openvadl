package vadl.viam.passes.translation_validation;

import java.io.StringWriter;
import java.util.Objects;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;
import vadl.types.UIntType;
import vadl.viam.Constant;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphNodeVisitor;
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
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;

public class Z3CodeGeneratorVisitor implements GraphNodeVisitor {
  private final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Value) {
      writer.write(String.format("BitVecVal(%d, %d)",
          ((Constant.Value) node.constant()).intValue(),
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
          if (node.builtIn() == BuiltInTable.EQU) {
            writer.write(" == ");
          } else {
            writer.write(" " + node.builtIn().operator() + " ");
          }
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
    var mem = writeMemNode.memory().identifier.simpleName();
    writer.write("Store(" + mem + ", ");
    writeMemNode.ensureNonNull(writeMemNode.address(), "Address must not be null");
    visit(Objects.requireNonNull(writeMemNode.address()));
    writer.write(", ");
    visit(writeMemNode.value());
    writer.write(")");
  }

  @Override
  public void visit(TypeCastNode typeCastNode) {
    if (typeCastNode.castType() instanceof UIntType uint) {
      var width = uint.bitWidth()
          - ((BitsType) typeCastNode.value().type()).bitWidth();
      typeCast(width, "ZeroExt(", typeCastNode, uint.bitWidth());
    } else if (typeCastNode.castType() instanceof SIntType sint) {
      var width = sint.bitWidth()
          - ((BitsType) typeCastNode.value().type()).bitWidth();
      typeCast(width, "SignExt(", typeCastNode, sint.bitWidth());
    } else if (typeCastNode.castType() instanceof BitsType bitsType) {
      var width = bitsType.bitWidth()
          - ((BitsType) typeCastNode.value().type()).bitWidth();
      typeCast(width, "SignExt(", typeCastNode, bitsType.bitWidth());
    } else {
      throw new RuntimeException("not supported type");
    }
  }

  /**
   * Creates z3 functions for typecasting.
   * There are three cases:
   * 1. The difference is larger than 0. Then, the target type's bit size is larger than the
   * source type's. We sign extend or zero extend based on the target type.
   * 2. The difference is smaller than 0. Then, the target type's bit size is smaller than the
   * source type's. We slice with the {@code Extract} function.
   * 3. The difference is zero. Then, no casting or slicing is required.
   *
   * @param relativeBitSizeDifference is the difference between the bit sizes of the target type and
   *                                  source type.
   * @param z3CastingOperation        defines the z3 function: whether sign extend or zero extend.
   * @param typeCastNode              is the {@link Node} which should be visited next.
   * @param targetBitSize             is the absolute bit size of the target type.
   */
  private void typeCast(int relativeBitSizeDifference, String z3CastingOperation,
                        TypeCastNode typeCastNode, int targetBitSize) {
    if (relativeBitSizeDifference > 0) {
      writer.write(z3CastingOperation + relativeBitSizeDifference + ", ");
      visit(typeCastNode.value());
      writer.write(")");
    } else if (relativeBitSizeDifference < 0) {
      writer.write("Extract(" + (targetBitSize - 1) + ", 0, ");
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
    writer.write(", ");
    visit(selectNode.trueCase());
    writer.write(", ");
    visit(selectNode.falseCase());
    writer.write(")");
  }

  @Override
  public void visit(ReadRegNode readRegNode) {
    writer.write(readRegNode.register().identifier.simpleName());
  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    // Do not write the register file because we actually care about the address.
    // a = X << X (wrong)
    // a = rs1 << rs2 (correct)
    writer.write("Select(" + readRegFileNode.registerFile().identifier.simpleName() + ", ");
    visit(readRegFileNode.address());
    writer.write(")");
  }

  @Override
  public void visit(ReadMemNode readMemNode) {
    var mem = readMemNode.memory().identifier.simpleName();
    writer.write("Select(" + mem + ", ");
    visit(readMemNode.address());
    writer.write(")");
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
    writer.write(fieldRefNode.formatField().identifier.simpleName());
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    writer.write(fieldAccessRefNode.fieldAccess().identifier.simpleName());
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
