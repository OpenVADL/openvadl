package vadl.lcb.codegen;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

class EncodingCodeGeneratorTest extends AbstractTest {
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
    String code = new EncodingCodeGenerator().generateFunction(function);

    // Then
    assertThat(code).isEqualToIgnoringWhitespace("""
        int32_t encodefunctionNameValue(uint32_t parameterValue) {
          return parameterValue;
        }
        """);
  }
}