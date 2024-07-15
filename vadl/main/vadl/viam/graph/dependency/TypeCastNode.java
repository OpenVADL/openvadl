package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.oop.SymbolTable;
import vadl.types.BoolType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;


/**
 * Represents a type cast in the VIAM graph.
 * A type cast node is a unary node that casts the value of its input node to a specified type.
 */
public class TypeCastNode extends UnaryNode {

  @DataValue
  private final Type castType;

  public TypeCastNode(ExpressionNode value, Type type) {
    super(value, type);
    this.castType = type;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(castType);
  }

  @Override
  public Node copy() {
    return new TypeCastNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new TypeCastNode(value, type());
  }

  @Override
  public String generateOopExpression(SymbolTable symbolTable) {
    if (castType instanceof BoolType) {
      return "(bool)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof SIntType && ((SIntType) castType).bitWidth() == 8) {
      return "(char)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof SIntType && ((SIntType) castType).bitWidth() == 16) {
      return "(short int)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof SIntType && ((SIntType) castType).bitWidth() == 32) {
      return "(long int)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof SIntType && ((SIntType) castType).bitWidth() == 64) {
      return "(long long int)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof UIntType && ((UIntType) castType).bitWidth() == 8) {
      return "(unsigned char)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof UIntType && ((UIntType) castType).bitWidth() == 16) {
      return "(unsigned short int)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof UIntType && ((UIntType) castType).bitWidth() == 32) {
      return "(unsigned long int)" + " " + value.generateOopExpression(symbolTable);
    } else if (castType instanceof UIntType && ((UIntType) castType).bitWidth() == 64) {
      return "(unsigned long long int)" + " " + value.generateOopExpression(symbolTable);
    } else {
      throw new RuntimeException("not implemented");
    }
  }
}
