package vadl.viam.helper;

import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class TestGraphUtils {

  public static BuiltInCall binaryOp(BuiltInTable.BuiltIn op, Constant.Value a, Constant.Value b) {
    return new BuiltInCall(
        op,
        new NodeList<>(
            new ConstantNode(a),
            new ConstantNode(b)
        ),
        a.type()
    );
  }

  public static TypeCastNode cast(ExpressionNode val, Type type) {
    return new TypeCastNode(val, type);
  }

  // constant value construction
  public static Constant.Value intS(long val, int width) {
    return Constant.Value.of(val, Type.signedInt(width));
  }

  public static Constant.Value intU(long val, int width) {
    return Constant.Value.of(val, Type.unsignedInt(width));
  }

  public static Constant.Value bits(long val, int width) {
    return Constant.Value.of(val, Type.bits(width));
  }

  public static Constant.Value bool(boolean val) {
    return Constant.Value.of(val);
  }

  public static Constant.Tuple.Status status(boolean negative, boolean zero, boolean carry,
                                             boolean overflow) {
    return new Constant.Tuple.Status(negative, zero, carry, overflow);
  }


}
