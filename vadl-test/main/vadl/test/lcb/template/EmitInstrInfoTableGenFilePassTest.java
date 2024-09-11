package vadl.test.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

public class EmitInstrInfoTableGenFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitInstrInfoTableGenFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitInstrInfoTableGenFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        /*
         * Node representing the frame index.
         * The method CPUDAGToDAGISel::SelectAddrFI is used to determine
         * if the specific register is a frame pointer.
         */
        def AddrFI : ComplexPattern<iPtr, 1, "SelectAddrFI", [frameindex], []>;
                
        def SDT_CallSeqStart : SDCallSeqStart<[SDTCisVT<0, i32>, SDTCisVT<1, i32>]>;
        def SDT_CallSeqEnd   : SDCallSeqEnd<[SDTCisVT<0, i32>, SDTCisVT<1, i32>]>;
                
                
                
        class RV64IM_Itype_immS_decode<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Itype_immS_encoding";
          let DecoderMethod = "RV64IM_Itype_immS_decode";
        }
                
        def RV64IM_Itype_immS_decodeAsInt64
            : RV64IM_Itype_immS_decode<i64>
            , ImmLeaf<i64, [{ return RV64IM_Itype_immS_predicate(Imm); }]>;
                
                
        class RV64IM_Btype_immS_decode<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Btype_immS_encoding";
          let DecoderMethod = "RV64IM_Btype_immS_decode";
        }
                
        def RV64IM_Btype_immS_decodeAsInt64
            : RV64IM_Btype_immS_decode<i64>
            , ImmLeaf<i64, [{ return RV64IM_Btype_immS_predicate(Imm); }]>;
                
                
        class RV64IM_Stype_immS_decode<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Stype_immS_encoding";
          let DecoderMethod = "RV64IM_Stype_immS_decode";
        }
                
        def RV64IM_Stype_immS_decodeAsInt64
            : RV64IM_Stype_immS_decode<i64>
            , ImmLeaf<i64, [{ return RV64IM_Stype_immS_predicate(Imm); }]>;
                
                
                
                
                
        def ADD : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(add X:$rs1, X:$rs2)
                (ADD X:$rs1, X:$rs2)>;
                
                
                
        def ADDI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
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
                
        def : Pat<(add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)
                (ADDI X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)
                (ADDI AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def AND : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(and X:$rs1, X:$rs2)
                (AND X:$rs1, X:$rs2)>;
                
                
                
        def ANDI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
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
                
        def : Pat<(and X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)
                (ANDI X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def BEQ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS );
                
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
                
        let Inst{11-8} = imm{3-0};
        let Inst{30-25} = imm{9-4};
        let Inst{7} = imm{10};
        let Inst{31} = imm{11};
        let Inst{24-20} = rs2{4-0};
        let Inst{19-15} = rs1{4-0};
        let Inst{14-12} = funct3{2-0};
        let Inst{6-0} = opcode{6-0};
                
        let isTerminator  = 1;
        let isBranch      = 1;
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
                
        def : Pat<(brcc (SETEQ X:$rs1, X:$rs2), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BEQ X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(brcond (i32 (seteq X:$rs1, X:$rs2)), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BEQ X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
                
                
        def BGE : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS );
                
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
                
        let Inst{11-8} = imm{3-0};
        let Inst{30-25} = imm{9-4};
        let Inst{7} = imm{10};
        let Inst{31} = imm{11};
        let Inst{24-20} = rs2{4-0};
        let Inst{19-15} = rs1{4-0};
        let Inst{14-12} = funct3{2-0};
        let Inst{6-0} = opcode{6-0};
                
        let isTerminator  = 1;
        let isBranch      = 1;
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
                
        def : Pat<(brcc (SETGE X:$rs1, X:$rs2), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BGE X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(brcond (i32 (setge X:$rs1, X:$rs2)), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BGE X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
                
                
        def BGEU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS );
                
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
                
        let Inst{11-8} = imm{3-0};
        let Inst{30-25} = imm{9-4};
        let Inst{7} = imm{10};
        let Inst{31} = imm{11};
        let Inst{24-20} = rs2{4-0};
        let Inst{19-15} = rs1{4-0};
        let Inst{14-12} = funct3{2-0};
        let Inst{6-0} = opcode{6-0};
                
        let isTerminator  = 1;
        let isBranch      = 1;
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
                
        def : Pat<(brcc (SETUGE X:$rs1, X:$rs2), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(brcond (i32 (setuge X:$rs1, X:$rs2)), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
                
                
        def BLT : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS );
                
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
                
        let Inst{11-8} = imm{3-0};
        let Inst{30-25} = imm{9-4};
        let Inst{7} = imm{10};
        let Inst{31} = imm{11};
        let Inst{24-20} = rs2{4-0};
        let Inst{19-15} = rs1{4-0};
        let Inst{14-12} = funct3{2-0};
        let Inst{6-0} = opcode{6-0};
                
        let isTerminator  = 1;
        let isBranch      = 1;
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
                
        def : Pat<(brcc (SETLT X:$rs1, X:$rs2), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BLT X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(brcond (i32 (setlt X:$rs1, X:$rs2)), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BLT X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
                
                
        def BLTU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS );
                
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
                
        let Inst{11-8} = imm{3-0};
        let Inst{30-25} = imm{9-4};
        let Inst{7} = imm{10};
        let Inst{31} = imm{11};
        let Inst{24-20} = rs2{4-0};
        let Inst{19-15} = rs1{4-0};
        let Inst{14-12} = funct3{2-0};
        let Inst{6-0} = opcode{6-0};
                
        let isTerminator  = 1;
        let isBranch      = 1;
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
                
        def : Pat<(brcc (SETULT X:$rs1, X:$rs2), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(brcond (i32 (setult X:$rs1, X:$rs2)), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
                
                
        def BNE : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS );
                
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
                
        let Inst{11-8} = imm{3-0};
        let Inst{30-25} = imm{9-4};
        let Inst{7} = imm{10};
        let Inst{31} = imm{11};
        let Inst{24-20} = rs2{4-0};
        let Inst{19-15} = rs1{4-0};
        let Inst{14-12} = funct3{2-0};
        let Inst{6-0} = opcode{6-0};
                
        let isTerminator  = 1;
        let isBranch      = 1;
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
                
        def : Pat<(brcc (SETNE X:$rs1, X:$rs2), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BNE X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(brcond (i32 (setne X:$rs1, X:$rs2)), RV64IM_Btype_immS_decodeAsInt64:$immS)
                (BNE X:$rs1, X:$rs2, RV64IM_Btype_immS_decodeAsInt64:$immS)>;
                
                
                
        def JALR : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
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
                
                
                
                
        def LB : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(i64 (sextloadi8 (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LB X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(i64 (sextloadi8 (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LB AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def LBU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
        bits<3> funct3 = 0b001;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(u64 (zextloadi8 (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LBU X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(u64 (zextloadi8 (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LBU AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def LD : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(i64 (load (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LD X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(i64 (load (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LD AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def LH : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
        bits<3> funct3 = 0b1;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(i64 (sextloadi16 (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LH X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(i64 (sextloadi16 (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LH AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def LHU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
        bits<3> funct3 = 0b101;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(u64 (zextloadi16 (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LHU X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(u64 (zextloadi16 (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LHU AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def LW : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(i64 (sextloadi32 (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LW X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(i64 (sextloadi32 (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LW AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def LWU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b11;
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
        let mayLoad       = 1;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(u64 (zextloadi32 (add X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LWU X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(u64 (zextloadi32 (add AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)))
                (LWU AddrFI:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def MUL : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(smul_lohi X:$rs1, X:$rs2)
                (MUL X:$rs1, X:$rs2)>;
                
                
                
        def MULH : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<
                (MULH X:$rs1, X:$rs2)>;
                
                
                
        def MULHSU : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<
                (MULHSU X:$rs1, X:$rs2)>;
                
                
                
        def MULW : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(smul_lohi X:$rs1, X:$rs2)
                (MULW X:$rs1, X:$rs2)>;
                
                
                
        def OR : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(or X:$rs1, X:$rs2)
                (OR X:$rs1, X:$rs2)>;
                
                
                
        def ORI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
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
                
        def : Pat<(or X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)
                (ORI X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def SB : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b110001;
        bits<3> funct3 = 0b;
        bits<12> imm;
        bits<5> rs2;
        bits<5> rs1;
                
        let Inst{11-7} = imm{4-0};
        let Inst{31-25} = imm{11-5};
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
        let mayStore      = 1;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(truncstorei8 X:$rs2, (add X:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SB X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(truncstorei8 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SB AddrFI:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
                
                
        def SD : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b110001;
        bits<3> funct3 = 0b11;
        bits<12> imm;
        bits<5> rs2;
        bits<5> rs1;
                
        let Inst{11-7} = imm{4-0};
        let Inst{31-25} = imm{11-5};
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
        let mayStore      = 1;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(store X:$rs2, (add X:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SD X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(store X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SD AddrFI:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
                
                
        def SH : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b110001;
        bits<3> funct3 = 0b1;
        bits<12> imm;
        bits<5> rs2;
        bits<5> rs1;
                
        let Inst{11-7} = imm{4-0};
        let Inst{31-25} = imm{11-5};
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
        let mayStore      = 1;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(truncstorei16 X:$rs2, (add X:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SH X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(truncstorei16 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SH AddrFI:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
                
                
        def SLT : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(setcc X:$rs1, X:$rs2, SETLT)
                (SLT X:$rs1, X:$rs2)>;
                
                
                
        def SLTI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
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
                
        def : Pat<(setcc X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS, SETLT)
                (SLTI X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def SLTIU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS );
                
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
                
        def : Pat<(setcc X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS, SETULT)
                (SLTIU X:$rs1, RV64IM_Itype_immS_decodeAsInt64:$immS)>;
                
                
                
        def SLTU : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(setcc X:$rs1, X:$rs2, SETULT)
                (SLTU X:$rs1, X:$rs2)>;
                
                
                
        def SUB : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(sub X:$rs1, X:$rs2)
                (SUB X:$rs1, X:$rs2)>;
                
                
                
        def SUBW : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(sub X:$rs1, X:$rs2)
                (SUBW X:$rs1, X:$rs2)>;
                
                
                
        def SW : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b110001;
        bits<3> funct3 = 0b01;
        bits<12> imm;
        bits<5> rs2;
        bits<5> rs1;
                
        let Inst{11-7} = imm{4-0};
        let Inst{31-25} = imm{11-5};
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
        let mayStore      = 1;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(truncstorei32 X:$rs2, (add X:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SW X:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
        def : Pat<(truncstorei32 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immS_decodeAsInt64:$immS))
                (SW AddrFI:$rs1, X:$rs2, RV64IM_Stype_immS_decodeAsInt64:$immS)>;
                
                
                
        def XOR : Instruction
        {
        let Namespace = "processorNameValue";
                
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
                
        def : Pat<(xor X:$rs1, X:$rs2)
                (XOR X:$rs1, X:$rs2)>;
        """.trim().lines(), output);
  }
}