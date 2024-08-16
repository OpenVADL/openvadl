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
        new TableGenImmediateOperand("nameValue",
            ValueType.I32);

    // When
    var result = TableGenImmediateOperandRenderer.lower(operand);

    // Then
    assertThat(result).isEqualToIgnoringWhitespace("""
               
        class nameValue<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "encodenameValue";
          let DecoderMethod = "decodenameValue";
        } 
        
        def nameValueAsInt32
              : nameValue<i32>
              , ImmLeaf<i32, [{ return true; }]>;
        """);
  }
}