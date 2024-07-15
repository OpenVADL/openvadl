package vadl.viam.graph.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import vadl.oop.SymbolTable;
import vadl.types.DataType;
import vadl.viam.Constant;

class ConstantNodeTest {
  @Test
  void shouldReturnNumberForOop() {
    var constant = new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);

    assertEquals("1", node.generateOopExpression(new SymbolTable()));
  }

  @Test
  void shouldReturnStringForOop() {
    var constant = new Constant.Str("testValue");
    var node = new ConstantNode(constant);

    assertEquals("testValue", node.generateOopExpression(new SymbolTable()));
  }
}