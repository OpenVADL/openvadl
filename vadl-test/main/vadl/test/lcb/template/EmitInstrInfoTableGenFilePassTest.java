package vadl.test.lcb.template;

import java.io.IOException;
import java.io.StringWriter;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.field_node_replacement.FieldNodeReplacementPassForDecoding;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.passes.FunctionInlinerPass;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class EmitInstrInfoTableGenFilePassTest extends AbstractTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var passManager = new PassManager();
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");

    passManager.add(new TypeCastEliminationPass());
    passManager.add(new FunctionInlinerPass());
    passManager.add(new IsaMatchingPass());
    passManager.add(new LlvmLoweringPass());

    passManager.run(spec);

    // When
    var template =
        new vadl.lcb.lib.Target.EmitInstrInfoTableGenFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(passManager.getPassResults(), spec, writer);
    var trimmed = writer.toString().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
class RV3264I_Itype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Itype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Itype_immS_decode_decode";
}

def RV3264I_Itype_immS_decodeAsInt64
    : RV3264I_Itype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Itype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Itype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Itype_immS_decode_decode";
}

def RV3264I_Itype_immS_decodeAsInt64
    : RV3264I_Itype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Btype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Btype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Btype_immS_decode_decode";
}

def RV3264I_Btype_immS_decodeAsInt64
    : RV3264I_Btype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Btype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Btype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Btype_immS_decode_decode";
}

def RV3264I_Btype_immS_decodeAsInt64
    : RV3264I_Btype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Btype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Btype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Btype_immS_decode_decode";
}

def RV3264I_Btype_immS_decodeAsInt64
    : RV3264I_Btype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Btype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Btype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Btype_immS_decode_decode";
}

def RV3264I_Btype_immS_decodeAsInt64
    : RV3264I_Btype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Btype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Btype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Btype_immS_decode_decode";
}

def RV3264I_Btype_immS_decodeAsInt64
    : RV3264I_Btype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Btype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Btype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Btype_immS_decode_decode";
}

def RV3264I_Btype_immS_decodeAsInt64
    : RV3264I_Btype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Jtype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Jtype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Jtype_immS_decode_decode";
}

def RV3264I_Jtype_immS_decodeAsInt64
    : RV3264I_Jtype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Itype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Itype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Itype_immS_decode_decode";
}

def RV3264I_Itype_immS_decodeAsInt64
    : RV3264I_Itype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Itype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Itype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Itype_immS_decode_decode";
}

def RV3264I_Itype_immS_decodeAsInt64
    : RV3264I_Itype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Itype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Itype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Itype_immS_decode_decode";
}

def RV3264I_Itype_immS_decodeAsInt64
    : RV3264I_Itype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;


class RV3264I_Itype_immS_decode<ValueType ty> : Operand<ty>
{
  let EncoderMethod = "RV3264I_Itype_immS_decode_encode";
  let DecoderMethod = "RV3264I_Itype_immS_decode_decode";
}

def RV3264I_Itype_immS_decodeAsInt64
    : RV3264I_Itype_immS_decode<i64>
    , ImmLeaf<i64, [{ return true; }]>;





def ADD : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b;
bits<7> funct7 = 0b;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def ADDI : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b11001;
bits<3> funct3 = 0b;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def AND : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b111;
bits<7> funct7 = 0b;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def ANDI : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b11001;
bits<3> funct3 = 0b111;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def BEQ : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{31-31} = imm{0-0};
let Inst{7-7} = imm{2-2};
let Inst{30-25} = imm{9-4};
let Inst{11-8} = imm{14-11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}


def BGE : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b101;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{31-31} = imm{0-0};
let Inst{7-7} = imm{2-2};
let Inst{30-25} = imm{9-4};
let Inst{11-8} = imm{14-11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}


def BGEU : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b111;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{31-31} = imm{0-0};
let Inst{7-7} = imm{2-2};
let Inst{30-25} = imm{9-4};
let Inst{11-8} = imm{14-11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}


def BLT : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b001;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{31-31} = imm{0-0};
let Inst{7-7} = imm{2-2};
let Inst{30-25} = imm{9-4};
let Inst{11-8} = imm{14-11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}


def BLTU : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b011;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{31-31} = imm{0-0};
let Inst{7-7} = imm{2-2};
let Inst{30-25} = imm{9-4};
let Inst{11-8} = imm{14-11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}


def BNE : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, X:$rs2, RV3264I_Btype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1100011;
bits<3> funct3 = 0b1;
bits<12> imm;
bits<5> rs2;
bits<5> rs1;

let Inst{31-31} = imm{0-0};
let Inst{7-7} = imm{2-2};
let Inst{30-25} = imm{9-4};
let Inst{11-8} = imm{14-11};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [ PC ];
}


def JAL : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins RV3264I_Jtype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1111011;
bits<20> imm;
bits<5> rd;

let Inst{31-31} = imm{0-0};
let Inst{19-12} = imm{9-2};
let Inst{20-20} = imm{11-11};
let Inst{30-21} = imm{22-13};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC ];
let Defs = [ PC ];
}


def JALR : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs  );
let InOperandList = ( ins X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b1110011;
bits<3> funct3 = 0b;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [ PC ];
let Defs = [ PC ];
}


def MUL : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b;
bits<7> funct7 = 0b1;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def MULH : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b1;
bits<7> funct7 = 0b1;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def MULHSU : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b01;
bits<7> funct7 = 0b1;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def MULW : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110111;
bits<3> funct3 = 0b;
bits<7> funct7 = 0b1;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def OR : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b011;
bits<7> funct7 = 0b;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def ORI : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b11001;
bits<3> funct3 = 0b011;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def SLT : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b01;
bits<7> funct7 = 0b;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def SLTI : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b11001;
bits<3> funct3 = 0b01;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def SLTIU : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, RV3264I_Itype_immS_decodeAsInt64:$immS );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b11001;
bits<3> funct3 = 0b11;
bits<12> imm;
bits<5> rs1;
bits<5> rd;

let Inst{31-20} = imm{11-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def SLTU : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b11;
bits<7> funct7 = 0b;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def SUB : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b;
bits<7> funct7 = 0b000001;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def SUBW : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110111;
bits<3> funct3 = 0b;
bits<7> funct7 = 0b000001;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}


def XOR : Instruction
{
let Namespace = "dummyNamespaceValue";

let Size = 4;
let CodeSize = 4;

let OutOperandList = ( outs X:$rd );
let InOperandList = ( ins X:$rs1, X:$rs2 );

field bits<32> Inst;

// SoftFail is a field the disassembler can use to provide a way for
// instructions to not match without killing the whole decode process. It is
// mainly used for ARM, but Tablegen expects this field to exist or it fails
// to build the decode table.
field bits<32> SoftFail = 0;

bits<7> opcode = 0b110011;
bits<3> funct3 = 0b001;
bits<7> funct7 = 0b;
bits<5> rs2;
bits<5> rs1;
bits<5> rd;

let Inst{31-25} = funct7{6-0};
let Inst{24-20} = rs2{4-0};
let Inst{19-15} = rs1{4-0};
let Inst{14-12} = funct3{2-0};
let Inst{11-7} = rd{4-0};
let Inst{6-0} = opcode{6-0};

let isTerminator  = 0;
let isBranch      = 0;
let isCall        = 0;
let isReturn      = 0;
let isPseudo      = 0;
let isCodeGenOnly = 0;
let mayLoad       = 0;
let mayStore      = 0;

let Constraints = "";
let AddedComplexity = 0;

let Pattern = [];

let Uses = [  ];
let Defs = [  ];
}
        """.trim().lines(), output);
  }
}
