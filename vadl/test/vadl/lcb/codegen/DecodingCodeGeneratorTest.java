package vadl.lcb.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.lcb.codegen.encoding.DecodingCodeGenerator;
import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FuncParamNode;

class DecodingCodeGeneratorTest extends AbstractTest {
  @Test
  void generateFunction_shouldGenerate() {
    // Given
    var function = new Function(createIdentifier("functionNameValue"),
        new Parameter[] {new Parameter(createIdentifier("parameterValue"), Type.unsignedInt(32))},
        Type.signedInt(32));
    var graph = new Graph("graphValue");
    var returnNode = new ReturnNode(new FuncParamNode(new Parameter(
        createIdentifier("parameterValue"), Type.unsignedInt(32)
    )));
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);

    // When
    String code = new DecodingCodeGenerator().generateFunction(function);

    // Then
    assertEquals("int32_t functionNameValue_decode(uint32_t parameterValue) {\n"
        + "return parameterValue;\n"
        + "}", code);
  }
}