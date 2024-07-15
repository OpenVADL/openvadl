package vadl.viam.graph.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.oop.SymbolTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.NodeList;

class FuncCallNodeTest extends AbstractTest {
  @Test
  void shouldCreateFunctionalWithOneVarForOop() {
    var constant = new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var funcCallNode =
        new FuncCallNode(new NodeList<>(node), new Function(createIdentifier("nameValue"),
            new Parameter[] {createParameter("parameterValue", DataType.unsignedInt(32))},
            DataType.unsignedInt(32)), DataType.unsignedInt(32));

    assertEquals("nameValue(1)", funcCallNode.generateOopExpression(new SymbolTable()));
  }

  @Test
  void shouldCreateFunctionalWithTwoVarForOop() {
    var constant = new Constant.Value(BigInteger.ONE, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var funcCallNode =
        new FuncCallNode(new NodeList<>(node, node), new Function(createIdentifier("nameValue"),
            new Parameter[] {createParameter("parameterValue", DataType.unsignedInt(32))},
            DataType.unsignedInt(32)), DataType.unsignedInt(32));

    assertEquals("nameValue(1,1)", funcCallNode.generateOopExpression(new SymbolTable()));
  }

}