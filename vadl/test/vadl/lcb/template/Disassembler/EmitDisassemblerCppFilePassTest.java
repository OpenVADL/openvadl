package vadl.lcb.template.Disassembler;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.lcb.AbstractLcbTest;

public class EmitDisassemblerCppFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitDisassemblerCppFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitDisassemblerCppFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #include "processornamevalueDisassembler.h"
        #include <iostream>
        #include "Utils/ImmediateUtils.h"
                
        #define DEBUG_TYPE "disassembler"
                
        using namespace llvm;
                
        processornamevalueDisassembler::processornamevalueDisassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian) : MCDisassembler(STI, Ctx), IsBigEndian(isBigEndian)
        {
        }
                
        /* == Register Classes == */
                
        static const unsigned XDecoderTable[] = {
         \s
            processornamevalue::X0,
            processornamevalue::X1,
            processornamevalue::X2,
            processornamevalue::X3,
            processornamevalue::X4,
            processornamevalue::X5,
            processornamevalue::X6,
            processornamevalue::X7,
            processornamevalue::X8,
            processornamevalue::X9,
            processornamevalue::X10,
            processornamevalue::X11,
            processornamevalue::X12,
            processornamevalue::X13,
            processornamevalue::X14,
            processornamevalue::X15,
            processornamevalue::X16,
            processornamevalue::X17,
            processornamevalue::X18,
            processornamevalue::X19,
            processornamevalue::X20,
            processornamevalue::X21,
            processornamevalue::X22,
            processornamevalue::X23,
            processornamevalue::X24,
            processornamevalue::X25,
            processornamevalue::X26,
            processornamevalue::X27,
            processornamevalue::X28,
            processornamevalue::X29,
            processornamevalue::X30,
            processornamevalue::X31
         \s
        };
                
                
        /* == Immediate Decoding == */
                
        DecodeStatus RV3264I_Btype_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_Btype_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_Ftype_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 63;
            Imm = RV3264I_Ftype_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_Itype_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_Itype_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_Jtype_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264I_Jtype_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_Rtype_shamt_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264I_Rtype_shamt_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_Stype_immS_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_Stype_immS_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus RV3264I_Utype_immUp_decode_wrapper(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264I_Utype_immUp_decode(Imm);
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
                
                
                
        static DecodeStatus DecodeXRegisterClass
            ( MCInst &Inst
            , uint64_t RegNo
            , uint64_t Address
            , const void *Decoder
            )
        {
            // check if register number is in range
            if( RegNo >= 32)
                return MCDisassembler::Fail;
                
            // access custom generated decoder table in register info
            Register reg = XDecoderTable[RegNo];
                
            // check if decoded register is valid
            if( reg == processornamevalue::NoRegister )
                return MCDisassembler::Fail;
                
            Inst.addOperand( MCOperand::createReg(reg) );
            return MCDisassembler::Success;
        }
                
                
        #include "processornamevalueGenDisassemblerTables.inc"
                
        DecodeStatus processornamevalueDisassembler::getInstruction(MCInst &MI, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CS) const
        {
            if (Bytes.size() < 4)
            {
                Size = 0;
                return MCDisassembler::Fail;
            }
                
            uint32_t Instr;
           \s
           \s
                if (IsBigEndian)
                {
                    Instr = support::endian::read32be(Bytes.data());
                }
                else
                {
                    Instr = support::endian::read32le(Bytes.data());
                }
           \s
                
            auto Result = decodeInstruction(DecoderTable32, MI, Instr, Address, this, STI);
            Size = 4;
            return Result;
        }
                
        static MCDisassembler *createprocessornamevalueDisassembler(const Target &T, const MCSubtargetInfo &STI, MCContext &Ctx)
        {
            return new processornamevalueDisassembler(STI, Ctx, processornamevalueBaseInfo::IsBigEndian());
        }
                
        extern "C" void LLVMInitializeprocessornamevalueDisassembler()
        {
            // Register Target Disassembler
            TargetRegistry::RegisterMCDisassembler(getTheprocessornamevalueTarget(), createprocessornamevalueDisassembler);
        }
        """.trim().lines(), output);
  }
}
