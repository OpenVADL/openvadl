package vadl.viam.passes.translation_validation;

import java.io.StringWriter;
import java.util.Objects;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.Constant;
import vadl.viam.Memory;
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
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor which lowers {@link Node} into z3 predicates.
 */
public class Z3CodeGeneratorVisitor implements GraphNodeVisitor {
  protected final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(ConstantNode node) {
    if (node.constant() instanceof Constant.Value value
        && value.type() instanceof BitsType bitsType) {
      writer.write(String.format("BitVecVal(%d, %d)",
          value.intValue(),
          bitsType.bitWidth()));
    } else if (node.constant() instanceof Constant.Value value
        && value.type() instanceof BoolType) {
      if (value.bool()) {
        writer.write("True");
      } else {
        writer.write("False");
      }
    } else {
      throw new ViamError("not implemented");
    }
  }


  @Override
  public void visit(BuiltInCall node) {
    var operator = Z3BuiltinTranslationMap.lower(node.builtIn());

    switch (operator.right()) {
      case INFIX -> {
        node.ensure(node.arguments().size() > 1,
            "This method only works for more than 1 arguments");
        for (int i = 0; i < node.arguments().size(); i++) {
          visit(node.arguments().get(i));

          // The last argument should not emit an operand.
          if (i < node.arguments().size() - 1) {
            writer.write(" " + operator.left().value() + " ");
          }
        }
      }
      case PREFIX -> {
        if (node.arguments().size() == 1) {
          writer.write(operator.left().value() + "(");
          visit(node.arguments().get(0));
          writer.write(operator.left().value() + ")");
        } else {
          writer.write(operator.left().value() + "(");
          for (int i = 0; i < node.arguments().size(); i++) {
            visit(node.arguments().get(i));

            // The last argument should not emit a comma
            if (i < node.arguments().size() - 1) {
              writer.write(", ");
            }
          }
          writer.write(")");
        }
      }
      default -> throw new ViamError("Operator not covered");
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
    // Z3's theory of array defines only `Store` for one position.
    // Usually, the memory will be defined for a byte. So writing
    // multiple bytes requires recursion.
    writeMemNode.ensureNonNull(writeMemNode.address(), "Address must not be null");
    writeMultipleBytes(writeMemNode.memory(), writeMemNode.address(), writeMemNode.value(),
        writeMemNode.words(), 0, 0);
  }

  @Override
  public void visit(TypeCastNode typeCastNode) {
    if (typeCastNode.castType() instanceof UIntType uint) {
      visit(typeCastNode, uint);
    } else if (typeCastNode.castType() instanceof SIntType sint) {
      visit(typeCastNode, sint);
    } else if (typeCastNode.castType() instanceof BitsType bitsType) {
      visit(typeCastNode, bitsType);
    } else {
      throw new RuntimeException("not supported type");
    }
  }

  private void visit(TypeCastNode typeCastNode, UIntType uintType) {
    typeCast("ZeroExt(", typeCastNode, uintType.bitWidth());
  }

  private void visit(TypeCastNode typeCastNode, SIntType sintType) {
    typeCast("SignExt(", typeCastNode, sintType.bitWidth());
  }

  private void visit(TypeCastNode typeCastNode, BitsType bitsType) {
    typeCast("SignExt(", typeCastNode, bitsType.bitWidth());
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
    visit(returnNode.value());
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
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  @Override
  public void visit(ZeroExtendNode node) {
    var diff = node.type().bitWidth() - getBitWidth(node.value().type());
    writer.write("ZeroExt(" + diff + ", ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(SignExtendNode node) {
    var diff = node.type().bitWidth() - getBitWidth(node.value().type());
    writer.write("SignExt(" + diff + ", ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(TruncateNode node) {
    var diff = getBitWidth(node.value().type()) - node.type().bitWidth() - 1;
    writer.write("Extract(" + diff);
    writer.write(", 0, ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
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
   * @param z3CastingOperation defines the z3 function: whether sign extend or zero extend.
   * @param typeCastNode       is the {@link Node} which should be visited next.
   * @param targetBitSize      is the absolute bit size of the target type.
   */
  private void typeCast(String z3CastingOperation,
                        TypeCastNode typeCastNode, int targetBitSize) {
    // `relativeBitSizeDifference` is the difference between the bit sizes of the target type
    // and source type because z3 works with relative values.
    int relativeBitSizeDifference = getBitwidthDifference(typeCastNode, targetBitSize);

    if (relativeBitSizeDifference > 0) {
      upcastType(relativeBitSizeDifference, z3CastingOperation, typeCastNode, targetBitSize);
    } else if (relativeBitSizeDifference < 0) {
      downcastType(typeCastNode, targetBitSize);
    } else {
      // Otherwise, no casting required
      visit(typeCastNode.value());
    }
  }


  private void upcastType(int relativeBitSizeDifference, String z3CastingOperation,
                          TypeCastNode typeCastNode, int targetBitSize) {
    writer.write(z3CastingOperation + relativeBitSizeDifference + ", ");
    var requiresBoolConversion = typeCastNode.value().type() instanceof BoolType
        && !(typeCastNode.castType() instanceof BoolType);
    if (requiresBoolConversion) {
      // z3 cannot handle implicit typecasts
      // we need to do it ourselves
      writer.write("If(");
      visit(typeCastNode.value());
      writer.write(
          String.format(", BitVecVal(1, %s), BitVecVal(0, %s))", targetBitSize, targetBitSize));
    } else {
      // No implicit bool casting required
      visit(typeCastNode.value());
    }
    writer.write(")");
  }

  private void downcastType(TypeCastNode typeCastNode, int targetBitSize) {
    writer.write("Extract(" + (targetBitSize - 1) + ", 0, ");
    visit(typeCastNode.value());
    writer.write(")");
  }

  private void writeMultipleBytes(Memory memory, ExpressionNode address, ExpressionNode value,
                                  int words, int wordsWritten, int low) {
    var mem = memory.identifier.simpleName();
    var hi = low + memory.wordSize() - 1;
    if (words == 1) {
      writer.write("Store(" + mem + ", ");
      visit(Objects.requireNonNull(address));
      writer.write(" + " + wordsWritten + ", ");
      writer.write("Extract(" + hi + ", " + low + ", ");
      visit(value);
      writer.write("))");
    } else if (words > 1) {
      writer.write("Store(");
      writeMultipleBytes(memory, address, value, words - 1, wordsWritten + 1,
          low + memory.wordSize());
      writer.write(", ");
      visit(Objects.requireNonNull(address));
      writer.write(" + " + wordsWritten + ", ");
      writer.write("Extract(" + hi + ", " + low + ", ");
      visit(value);
      writer.write("))");
    }
  }

  private static int getBitwidthDifference(TypeCastNode typeCastNode, int bitsType) {
    if (typeCastNode.value().type() instanceof BitsType valBitsType) {
      return bitsType
          - valBitsType.bitWidth();
    } else if (typeCastNode.value().type() instanceof BoolType) {
      return bitsType - 1;
    }

    throw new ViamError("not implemented for other types");
  }

  private int getBitWidth(Type type) {
    if (type instanceof BitsType b) {
      return b.bitWidth();
    } else if (type instanceof BoolType b) {
      return b.bitWidth();
    }

    throw new RuntimeException("not supported");
  }
}
