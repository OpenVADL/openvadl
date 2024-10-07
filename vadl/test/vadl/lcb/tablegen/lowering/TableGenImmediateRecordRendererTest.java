package vadl.lcb.tablegen.lowering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenImmediateOperandRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;

class TableGenImmediateRecordRendererTest extends AbstractTest {

  @Test
  void shouldRenderOperand() {
    // Given
    var operand =
        new TableGenImmediateRecord(createIdentifier("nameValue"),
            createIdentifier("nameValue_encode"),
            createIdentifier("nameValue_decode"),
            createIdentifier("nameValue_predicate"),
            ValueType.I32,
            createFieldAccess("fieldAccessValue",
                new Function(createIdentifier("functionValue"), new Parameter[] {},
                    Type.dummy(), new Graph("graphValue"))));

    // When
    var result = TableGenImmediateOperandRenderer.lower(operand);

    // Then
    assertThat(result).isEqualToIgnoringWhitespace("""
                
        class nameValue<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "nameValue_encode";
          let DecoderMethod = "nameValue_decode";
        }
                
        def nameValueAsInt32
            : nameValue<i32>
            , ImmLeaf<i32, [{ return nameValue_predicate(Imm); }]>;
                
        def nameValueAsLabel : nameValue<OtherVT>;
        """);
  }
}