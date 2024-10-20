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
        def AddrFI : ComplexPattern<iPTR, 1, "SelectAddrFI", [frameindex], []>;
                
        def SDT_CallSeqStart : SDCallSeqStart<[SDTCisVT<0, i64>, SDTCisVT<1, i64>]>;
        def SDT_CallSeqEnd   : SDCallSeqEnd<[SDTCisVT<0, i64>, SDTCisVT<1, i64>]>;
                
        // Target-dependent type requirements
        def SDT_CPU_Call : SDTypeProfile<0, -1, [SDTCisVT<0, i64>]>;
                
        // Target-independent nodes, but with target-specific formats
        def callseq_start : SDNode<"ISD::CALLSEQ_START", SDT_CallSeqStart, [SDNPHasChain, SDNPOutGlue]>;
        def callseq_end   : SDNode<"ISD::CALLSEQ_END", SDT_CallSeqEnd, [SDNPHasChain, SDNPOptInGlue, SDNPOutGlue]>;
                
        def target_ret_flag : SDNode<"rv64imISD::RET_FLAG", SDTNone,
                                   [SDNPHasChain, SDNPOptInGlue, SDNPVariadic]>;
                
        /*
         * ADJCALLSTACKDOWN is a pseudo instruction used to represent the
         * 'CFSetupOpcode', which is needed for the call frame setup
         */
        def ADJCALLSTACKDOWN : Instruction
        {
            let InOperandList = (ins i64imm:$amt1, i64imm:$amt2); /* i32imm : Operand<i32> */
            let OutOperandList = (outs);
            let Pattern = [ (callseq_start timm:$amt1, timm:$amt2) ];
            let Namespace = "rv64im";
            let isPseudo = 1;
            let isCodeGenOnly = 1;
            let Defs = [ X2 ]; // stack pointer
            let Uses = [ X2 ]; // stack pointer
        }
                
        /*
         * ADJCALLSTACKUP is a pseudo instruction used to represent the
         * 'CFDestroyOpcode', which is needed for the call frame setup
         */
        def ADJCALLSTACKUP : Instruction
        {
            let InOperandList = (ins i64imm:$amt1, i64imm:$amt2);
            let OutOperandList = (outs);
            let Pattern = [ (callseq_end timm:$amt1, timm:$amt2) ];
            let Namespace = "rv64im";
            let isPseudo = 1;
            let isCodeGenOnly = 1;
            let Defs = [ X2 ]; // stack pointer
            let Uses = [ X2 ]; // stack pointer
        }
                
        def RESERVERD_PSEUDO_RET : Instruction
        {
            let Namespace = "rv64im";
            /* let Size = 4; // ( Bits<32> )
            let CodeSize = 4; // ( Bits<32> ), used for ISEL cost */
            let InOperandList = (ins);
            let OutOperandList = (outs);
            let Pattern =  [ (target_ret_flag) ];
            let isTerminator  = 1;
            let isBranch      = 0;
            let isCall        = 0;
            let isReturn      = 1;
            let isPseudo      = 1;
            let isCodeGenOnly = 1;
            let mayLoad       = 0;
            let mayStore      = 0;
            let Defs = [];
            let Uses = [];
        }
                
        /*
         * PSEUDO_CALL is a pseudo instruction used to represent the
         * 'target_call', which marks a function call.
         * It will be later expanded into the defined calling sequence during code emission.
         */
        def PSEUDO_CALL : Instruction
        {
            let Namespace = "rv64im";
            //let Size = 8; // ( Bits<32>, Bits<32> )
            //let CodeSize = 8; // ( Bits<32>, Bits<32> ), used for ISEL cost
            let InOperandList = (ins bare_symbol:$addr);
            let OutOperandList = (outs);
            let Pattern = [];
            let isTerminator  = 0;
            let isBranch      = 0;
            let isCall        = 1;
            let isReturn      = 0;
            let isPseudo      = 1;
            let isCodeGenOnly = 1;
            let mayLoad       = 0;
            let mayStore      = 0;
            let Defs = [];
            let Uses = [];
        }
                
        /* Match the call sequence for global and external symbols */
        def : Pat<(target_call tglobaladdr:$func), (PSEUDO_CALL tglobaladdr:$func)>;
        def : Pat<(target_call texternalsym:$func), (PSEUDO_CALL texternalsym:$func)>;
                
                
        class RV64IM_Ftype_sft<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Ftype_shamt_encoding_wrapper";
          let DecoderMethod = "RV64IM_Ftype_shamt_decode_wrapper";
        }
                
        def RV64IM_Ftype_sftAsInt64
            : RV64IM_Ftype_sft<i64>
            , ImmLeaf<i64, [{ return RV64IM_Ftype_shamt_predicate(Imm); }]>;
                
        def RV64IM_Ftype_sftAsLabel : RV64IM_Ftype_sft<OtherVT>;
                
        class RV64IM_Btype_imm<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Btype_immS_encoding_wrapper";
          let DecoderMethod = "RV64IM_Btype_immS_decode_wrapper";
        }
                
        def RV64IM_Btype_immAsInt64
            : RV64IM_Btype_imm<i64>
            , ImmLeaf<i64, [{ return RV64IM_Btype_immS_predicate(Imm); }]>;
                
        def RV64IM_Btype_immAsLabel : RV64IM_Btype_imm<OtherVT>;
                
        class RV64IM_Stype_imm<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Stype_immS_encoding_wrapper";
          let DecoderMethod = "RV64IM_Stype_immS_decode_wrapper";
        }
                
        def RV64IM_Stype_immAsInt64
            : RV64IM_Stype_imm<i64>
            , ImmLeaf<i64, [{ return RV64IM_Stype_immS_predicate(Imm); }]>;
                
        def RV64IM_Stype_immAsLabel : RV64IM_Stype_imm<OtherVT>;
                
        class RV64IM_Rtype_rs2<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Rtype_shamt_encoding_wrapper";
          let DecoderMethod = "RV64IM_Rtype_shamt_decode_wrapper";
        }
                
        def RV64IM_Rtype_rs2AsInt64
            : RV64IM_Rtype_rs2<i64>
            , ImmLeaf<i64, [{ return RV64IM_Rtype_shamt_predicate(Imm); }]>;
                
        def RV64IM_Rtype_rs2AsLabel : RV64IM_Rtype_rs2<OtherVT>;
                
        class RV64IM_Itype_imm<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Itype_immS_encoding_wrapper";
          let DecoderMethod = "RV64IM_Itype_immS_decode_wrapper";
        }
                
        def RV64IM_Itype_immAsInt64
            : RV64IM_Itype_imm<i64>
            , ImmLeaf<i64, [{ return RV64IM_Itype_immS_predicate(Imm); }]>;
                
        def RV64IM_Itype_immAsLabel : RV64IM_Itype_imm<OtherVT>;
                
        class RV64IM_Utype_imm<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Utype_immU_encoding_wrapper";
          let DecoderMethod = "RV64IM_Utype_immU_decode_wrapper";
        }
                
        def RV64IM_Utype_immAsInt64
            : RV64IM_Utype_imm<i64>
            , ImmLeaf<i64, [{ return RV64IM_Utype_immU_predicate(Imm); }]>;
                
        def RV64IM_Utype_immAsLabel : RV64IM_Utype_imm<OtherVT>;
                
        class RV64IM_Jtype_imm<ValueType ty> : Operand<ty>
        {
          let EncoderMethod = "RV64IM_Jtype_immS_encoding_wrapper";
          let DecoderMethod = "RV64IM_Jtype_immS_decode_wrapper";
        }
                
        def RV64IM_Jtype_immAsInt64
            : RV64IM_Jtype_imm<i64>
            , ImmLeaf<i64, [{ return RV64IM_Jtype_immS_predicate(Imm); }]>;
                
        def RV64IM_Jtype_immAsLabel : RV64IM_Jtype_imm<OtherVT>;
                
                
                
                
        def RV64IM_Ftype_sft_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Ftype_sftAsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Ftype_sftAsInt64:$imm),
                (RV64IM_Ftype_sft_const_mat RV64IM_Ftype_sftAsInt64:$imm)>;
                
                
        def RV64IM_Btype_imm_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Btype_immAsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Btype_immAsInt64:$imm),
                (RV64IM_Btype_imm_const_mat RV64IM_Btype_immAsInt64:$imm)>;
                
                
        def RV64IM_Stype_imm_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Stype_immAsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Stype_immAsInt64:$imm),
                (RV64IM_Stype_imm_const_mat RV64IM_Stype_immAsInt64:$imm)>;
                
                
        def RV64IM_Rtype_rs2_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Rtype_rs2AsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Rtype_rs2AsInt64:$imm),
                (RV64IM_Rtype_rs2_const_mat RV64IM_Rtype_rs2AsInt64:$imm)>;
                
                
        def RV64IM_Itype_imm_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Itype_immAsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Itype_immAsInt64:$imm),
                (RV64IM_Itype_imm_const_mat RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def RV64IM_Utype_imm_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Utype_immAsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Utype_immAsInt64:$imm),
                (RV64IM_Utype_imm_const_mat RV64IM_Utype_immAsInt64:$imm)>;
                
                
        def RV64IM_Jtype_imm_const_mat : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins RV64IM_Jtype_immAsInt64:$imm );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 1;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(RV64IM_Jtype_immAsInt64:$imm),
                (RV64IM_Jtype_imm_const_mat RV64IM_Jtype_immAsInt64:$imm)>;
                
                
                
                
                
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b000;
        bits<7> funct7 = 0b0000000;
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
                
        def : Pat<(add X:$rs1, X:$rs2),
                (ADD X:$rs1, X:$rs2)>;
                
                
        def ADDI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100100;
        bits<3> funct3 = 0b000;
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
                
        def : Pat<(add X:$rs1, RV64IM_Itype_immAsInt64:$imm),
                (ADDI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm),
                (ADDI AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b111;
        bits<7> funct7 = 0b0000000;
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
                
        def : Pat<(and X:$rs1, X:$rs2),
                (AND X:$rs1, X:$rs2)>;
                
                
        def ANDI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100100;
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
                
        def : Pat<(and X:$rs1, RV64IM_Itype_immAsInt64:$imm),
                (ANDI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def BEQ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100011;
        bits<3> funct3 = 0b000;
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
                
        def : Pat<(brcc SETEQ, X:$rs1, X:$rs2, bb:$imm),
                (BEQ X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
        def : Pat<(brcond (i64 (seteq X:$rs1, X:$rs2)), bb:$imm),
                (BEQ X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
                
        def BGE : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );
                
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
                
        def : Pat<(brcc SETGE, X:$rs1, X:$rs2, bb:$imm),
                (BGE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
        def : Pat<(brcond (i64 (setge X:$rs1, X:$rs2)), bb:$imm),
                (BGE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
                
        def BGEU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );
                
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
                
        def : Pat<(brcc SETUGE, X:$rs1, X:$rs2, bb:$imm),
                (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
        def : Pat<(brcond (i64 (setuge X:$rs1, X:$rs2)), bb:$imm),
                (BGEU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
                
        def BLT : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );
                
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
                
        def : Pat<(brcc SETLT, X:$rs1, X:$rs2, bb:$imm),
                (BLT X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
        def : Pat<(brcond (i64 (setlt X:$rs1, X:$rs2)), bb:$imm),
                (BLT X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
                
        def BLTU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );
                
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
                
        def : Pat<(brcc SETULT, X:$rs1, X:$rs2, bb:$imm),
                (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
        def : Pat<(brcond (i64 (setult X:$rs1, X:$rs2)), bb:$imm),
                (BLTU X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
                
        def BNE : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100011;
        bits<3> funct3 = 0b100;
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
                
        def : Pat<(brcc SETNE, X:$rs1, X:$rs2, bb:$imm),
                (BNE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
        def : Pat<(brcond (i64 (setne X:$rs1, X:$rs2)), bb:$imm),
                (BNE X:$rs1, X:$rs2, RV64IM_Btype_immAsLabel:$imm)>;
                
                
        def JALR : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1110011;
        bits<3> funct3 = 0b000;
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
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
        bits<3> funct3 = 0b000;
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
                
        def : Pat<(i64 (sextloadi8 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LB X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (sextloadi8 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LB AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def LBU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
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
                
        def : Pat<(i64 (zextloadi8 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LBU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (zextloadi8 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LBU AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def LD : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
        bits<3> funct3 = 0b110;
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
                
        def : Pat<(i64 (load (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LD X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (load (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LD AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def LH : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
        bits<3> funct3 = 0b100;
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
                
        def : Pat<(i64 (sextloadi16 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LH X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (sextloadi16 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LH AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def LHU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
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
                
        def : Pat<(i64 (zextloadi16 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LHU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (zextloadi16 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LHU AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def LW : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
        bits<3> funct3 = 0b010;
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
                
        def : Pat<(i64 (sextloadi32 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LW X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (sextloadi32 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LW AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def LWU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100000;
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
                
        def : Pat<(i64 (zextloadi32 (add X:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LWU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
        def : Pat<(i64 (zextloadi32 (add AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm))),
                (LWU AddrFI:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b000;
        bits<7> funct7 = 0b1000000;
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
                
        def : Pat<(smullohi X:$rs1, X:$rs2),
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b100;
        bits<7> funct7 = 0b1000000;
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b010;
        bits<7> funct7 = 0b1000000;
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
                
        bits<7> opcode = 0b1101110;
        bits<3> funct3 = 0b000;
        bits<7> funct7 = 0b1000000;
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
                
        def : Pat<(smullohi X:$rs1, X:$rs2),
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b011;
        bits<7> funct7 = 0b0000000;
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
                
        def : Pat<(or X:$rs1, X:$rs2),
                (OR X:$rs1, X:$rs2)>;
                
                
        def ORI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100100;
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
                
        def : Pat<(or X:$rs1, RV64IM_Itype_immAsInt64:$imm),
                (ORI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def SB : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100010;
        bits<3> funct3 = 0b000;
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
                
        def : Pat<(truncstorei8 X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SB X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
        def : Pat<(truncstorei8 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SB AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
                
        def SD : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100010;
        bits<3> funct3 = 0b110;
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
                
        def : Pat<(store X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SD X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
        def : Pat<(store X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SD AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
                
        def SH : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100010;
        bits<3> funct3 = 0b100;
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
                
        def : Pat<(truncstorei16 X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SH X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
        def : Pat<(truncstorei16 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SH AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
                
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b010;
        bits<7> funct7 = 0b0000000;
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
                
        def : Pat<(setcc X:$rs1, X:$rs2, SETLT),
                (SLT X:$rs1, X:$rs2)>;
                
                
        def SLTI : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100100;
        bits<3> funct3 = 0b010;
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
                
        def : Pat<(setcc X:$rs1, RV64IM_Itype_immAsInt64:$imm, SETLT),
                (SLTI X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
        def SLTIU : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1, RV64IM_Itype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100100;
        bits<3> funct3 = 0b110;
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
                
        def : Pat<(setcc X:$rs1, RV64IM_Itype_immAsInt64:$imm, SETULT),
                (SLTIU X:$rs1, RV64IM_Itype_immAsInt64:$imm)>;
                
                
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b110;
        bits<7> funct7 = 0b0000000;
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
                
        def : Pat<(setcc X:$rs1, X:$rs2, SETULT),
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b000;
        bits<7> funct7 = 0b0000010;
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
                
        def : Pat<(sub X:$rs1, X:$rs2),
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
                
        bits<7> opcode = 0b1101110;
        bits<3> funct3 = 0b000;
        bits<7> funct7 = 0b0000010;
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
                
        def : Pat<(sub X:$rs1, X:$rs2),
                (SUBW X:$rs1, X:$rs2)>;
                
                
        def SW : Instruction
        {
        let Namespace = "processorNameValue";
                
        let Size = 4;
        let CodeSize = 4;
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm );
                
        field bits<32> Inst;
                
        // SoftFail is a field the disassembler can use to provide a way for
        // instructions to not match without killing the whole decode process. It is
        // mainly used for ARM, but Tablegen expects this field to exist or it fails
        // to build the decode table.
        field bits<32> SoftFail = 0;
                
        bits<7> opcode = 0b1100010;
        bits<3> funct3 = 0b010;
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
                
        def : Pat<(truncstorei32 X:$rs2, (add X:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SW X:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
        def : Pat<(truncstorei32 X:$rs2, (add AddrFI:$rs1, RV64IM_Stype_immAsInt64:$imm)),
                (SW AddrFI:$rs1, X:$rs2, RV64IM_Stype_immAsInt64:$imm)>;
                
                
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
                
        bits<7> opcode = 0b1100110;
        bits<3> funct3 = 0b001;
        bits<7> funct7 = 0b0000000;
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
                
        def : Pat<(xor X:$rs1, X:$rs2),
                (XOR X:$rs1, X:$rs2)>;
                
                
                
                
                
        def BEQZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def BGEZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def BGTZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def BLEZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def BLTZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def BNEZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def CALL : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [ PC,X1 ];
        let Defs = [ PC,X1 ];
        }
                
                
                
        def J : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def MOV : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1 );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(add X:$rs1, (i64 0)),
                (MOV X:$rs1)>;
                
                
        def NEG : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1 );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(sub (i64 0), X:$rs1),
                (NEG X:$rs1)>;
                
                
        def NOP : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [ X0 ];
        let Defs = [ X0 ];
        }
                
        def : Pat<(add (i64 0), (i64 0)),
                (NOP )>;
                
                
        def NOT : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
                
                
        def RET : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [ PC,X1 ];
        let Defs = [ PC,X0 ];
        }
                
                
                
        def SGTZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1 );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(setcc (i64 0), X:$rs1, SETLT),
                (SGTZ X:$rs1)>;
                
                
        def SLTZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1 );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(setcc X:$rs1, (i64 0), SETLT),
                (SLTZ X:$rs1)>;
                
                
        def SNEZ : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs X:$rd );
        let InOperandList = ( ins X:$rs1 );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [  ];
        let Defs = [  ];
        }
                
        def : Pat<(setcc (i64 0), X:$rs1, SETULT),
                (SNEZ X:$rs1)>;
                
                
        def TAIL : Instruction
        {
        let Namespace = "processorNameValue";
                
        let OutOperandList = ( outs  );
        let InOperandList = ( ins  );
                
        let isTerminator  = 0;
        let isBranch      = 0;
        let isCall        = 0;
        let isReturn      = 0;
        let isPseudo      = 1;
        let isCodeGenOnly = 0;
        let mayLoad       = 0;
        let mayStore      = 0;
                
        let Constraints = "";
        let AddedComplexity = 0;
                
        let Pattern = [];
                
        let Uses = [ PC,X6 ];
        let Defs = [ PC,X0 ];
        }
        """.trim().lines(), output);
  }
}