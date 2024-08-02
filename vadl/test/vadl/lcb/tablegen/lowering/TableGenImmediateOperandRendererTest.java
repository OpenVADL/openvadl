package vadl.lcb.tablegen.lowering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.tablegen.model.TableGenImmediateOperand;

class TableGenImmediateOperandRendererTest {

  @Test
  void shouldRenderOperand() {
    // Given
    var operand =
        new TableGenImmediateOperand("nameValue", "encoderMethodValue", "decoderMethodValue",
            ValueType.I32);

    // When
    var result = TableGenImmediateOperandRenderer.lower(operand);

    // Then
    assertThat(result).isEqualTo("""
               
        class nameValue<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "encoderMethodValue";
          let DecoderMethod = "decoderMethodValue";
        } 
        """);
  }
}