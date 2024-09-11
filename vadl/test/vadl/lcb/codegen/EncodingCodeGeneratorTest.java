package vadl.lcb.codegen;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.cppCodeGen.model.CppFunction;
import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FuncParamNode;

class EncodingCodeGeneratorTest extends AbstractTest {
  @Test
  void generateFunction_shouldGenerate() {
    // Given
    var graph = new Graph("graphValue");
    var returnNode = new ReturnNode(new FuncParamNode(new Parameter(
        createIdentifier("parameterValue"), Type.unsignedInt(32)
    )));
    graph.addWithInputs(returnNode);
    var function = new CppFunction(createIdentifier("functionNameValue"),
        new Parameter[] {new Parameter(createIdentifier("parameterValue"), Type.unsignedInt(32))},
        Type.signedInt(32), graph);

    // When
    String code = new CodeGenerator().generateFunction(function).value();

    // Then
    assertThat(code).isEqualToIgnoringWhitespace("""
        int32_t functionNameValue(uint32_t parameterValue) {
          return parameterValue;
        }
        """);
  }
}