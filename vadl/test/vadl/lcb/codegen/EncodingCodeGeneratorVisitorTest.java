package vadl.lcb.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;

class EncodingCodeGeneratorVisitorTest extends AbstractTest {
  StringWriter writer;
  EncoderDecoderCodeGeneratorVisitor visitor;

  @BeforeEach
  void beforeEach() {
    writer = new StringWriter();
    visitor = new EncoderDecoderCodeGeneratorVisitor(writer);
  }

  @Test
  void constant_shouldReturnNumber() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);

    // When
    visitor.visit(node);

    // Then
    assertEquals("1", writer.toString());
  }

  @Test
  void constant_shouldReturnString() {
    var constant = new Constant.Str("testValue");
    var node = new ConstantNode(constant);

    // When
    visitor.visit(node);

    assertEquals("testValue", writer.toString());
  }

  @Test
  void funcCallNode_shouldCreateFunctionalWithOneVar() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var funcCallNode =
        new FuncCallNode(new NodeList<>(node), new Function(createIdentifier("nameValue"),
            new Parameter[] {createParameter("parameterValue", DataType.unsignedInt(32))},
            DataType.unsignedInt(32)), DataType.unsignedInt(32));

    // When
    visitor.visit(funcCallNode);

    // Then
    assertEquals("nameValue(1)", writer.toString());
  }

  @Test
  void funcCallNode_shouldCreateFunctionalWithTwoVar() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var funcCallNode =
        new FuncCallNode(new NodeList<>(node, node), new Function(createIdentifier("nameValue"),
            new Parameter[] {createParameter("parameterValue", DataType.unsignedInt(32))},
            DataType.unsignedInt(32)), DataType.unsignedInt(32));

    // When
    visitor.visit(funcCallNode);

    // Then
    assertEquals("nameValue(1,1)", writer.toString());
  }

  @Test
  void builtIn_shouldGenerateCpp() {
    var constant = Constant.Value.of(1, DataType.unsignedInt(32));
    var node = new ConstantNode(constant);
    var builtIn =
        new BuiltInCall(BuiltInTable.ADD, new NodeList<>(node, node), DataType.unsignedInt(32));

    // When
    visitor.visit(builtIn);

    // Then
    assertEquals("(1) + (1)", writer.toString());
  }
}