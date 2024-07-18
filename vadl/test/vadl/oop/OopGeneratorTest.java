package vadl.oop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.TypeCastNode;

class OopGeneratorTest extends AbstractTest {
  @Test
  void generateFunction_shouldGenerate() {
    // Given
    var function = new Function(createIdentifier("functionNameValue"),
        new Parameter[] {new Parameter(createIdentifier("parameterValue"), Type.unsignedInt(32))},
        Type.signedInt(32));
    var graph = new Graph("graphValue");
    var returnNode = new ReturnNode(new TypeCastNode(new FuncParamNode(new Parameter(
        createIdentifier("parameterValue"), Type.unsignedInt(32)
    )), Type.signedInt(32)));
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);

    // When
    String code = new OopGenerator().generateFunction(function);

    // Then
    assertEquals("int32_t functionNameValue(uint32_t parameterValue) {\n"
        + "return (int32_t) parameterValue;\n"
        + "}", code);
  }
}