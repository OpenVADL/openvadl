package vadl.lcb.tablegen.lowering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.lowering.TableGenInstructionRenderer;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

class TableGenInstructionRendererTest extends AbstractTest {

  @Test
  void shouldRenderInstruction() {
    // Given
    var name = "nameValue";
    var namespace = "namespaceValue";
    var format = new Format(createIdentifier("formatValue"), BitsType.bits(32));
    var encodedField = new Format.Field(createIdentifier("opCode"), DataType.bits(10),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(9, 0)}),
        format);
    var noneEncodedField = new Format.Field(createIdentifier("register"), DataType.bits(10),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 10)}),
        format);
    format.setFields(
        new Format.Field[] {encodedField, noneEncodedField});
    var fields = new Encoding.Field[] {new Encoding.Field(createIdentifier("opCode"), encodedField,
        Constant.Value.of(10, DataType.bits(10)))};
    var viamInstruction = new Instruction(createIdentifier("ADD"), new Graph("graphValue"),
        createAssembly("assembly"), new Encoding(
        createIdentifier("encoding"), format, fields));
    var flags = new LlvmLoweringPass.Flags(
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false
    );
    var instruction =
        new TableGenMachineInstruction(name,
            namespace,
            viamInstruction,
            flags,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

    // When
    var result = TableGenInstructionRenderer.lower(instruction);

    // Then
    assertThat(result).isEqualToIgnoringWhitespace("""
        def nameValue : Instruction
        {
        let Namespace = "namespaceValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<10> opCode = 0b0101000000;
        bits<10> register;
                
        let Inst{19-10} = register{9-0};
        let Inst{9-0} = opCode{9-0};
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 0;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
        let isBarrier     = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
        """);
  }

}