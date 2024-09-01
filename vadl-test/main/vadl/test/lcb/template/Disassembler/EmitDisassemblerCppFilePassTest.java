package vadl.test.lcb.template.Disassembler;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerCppFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.test.lcb.AbstractLcbTest;

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
        #include "rv64imDisassembler.h"
        #include <iostream>
        #include "Utils/ImmediateUtils.h"
                
        #define DEBUG_TYPE "disassembler"
                
        using namespace llvm;
                
        rv64imDisassembler::rv64imDisassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian) : MCDisassembler(STI, Ctx), IsBigEndian(isBigEndian)
        {
        }
                
        /* == Register Classes == */
                
        static const unsigned XDecoderTable[] = {
         \s
            rv64im::X0,
            rv64im::X1,
            rv64im::X2,
            rv64im::X3,
            rv64im::X4,
            rv64im::X5,
            rv64im::X6,
            rv64im::X7,
            rv64im::X8,
            rv64im::X9,
            rv64im::X10,
            rv64im::X11,
            rv64im::X12,
            rv64im::X13,
            rv64im::X14,
            rv64im::X15,
            rv64im::X16,
            rv64im::X17,
            rv64im::X18,
            rv64im::X19,
            rv64im::X20,
            rv64im::X21,
            rv64im::X22,
            rv64im::X23,
            rv64im::X24,
            rv64im::X25,
            rv64im::X26,
            rv64im::X27,
            rv64im::X28,
            rv64im::X29,
            rv64im::X30,
            rv64im::X31
         \s
        };
                
                
        /* == Immediate Decoding == */
                
        DecodeStatus decodeRV3264I_Ftype_sft(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 63;
            Imm = RV3264I_Ftype_shamt_decode_decode;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus decodeRV3264I_Btype_imm(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_Btype_immS_decode_decode;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus decodeRV3264I_Stype_imm(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_Stype_immS_decode_decode;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus decodeRV3264I_Rtype_rs2(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 31;
            Imm = RV3264I_Rtype_shamt_decode_decode;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus decodeRV3264I_Itype_imm(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 4095;
            Imm = RV3264I_Itype_immS_decode_decode;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus decodeRV3264I_Utype_imm(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264I_Utype_immU_decode_decode;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
        DecodeStatus decodeRV3264I_Jtype_imm(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 1048575;
            Imm = RV3264I_Jtype_immS_decode_decode;
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
            Register reg = XDecoderTableName[RegNo];
                
            // check if decoded register is valid
            if( reg == rv64im::NoRegister )
                return MCDisassembler::Fail;
                
            Inst.addOperand( MCOperand::createReg(reg) );
            return MCDisassembler::Success;
        }
                
                
        #include "rv64imGenDisassemblerTables.inc"
                
            DecodeStatus rv64imDisassembler::getInstruction(MCInst &MI, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CS) const
        {
            if (Bytes.size() < 4)
            {
                Size = 0;
                return MCDisassembler::Fail;
            }
                
            uint4_t Instr;
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
                
        static MCDisassembler *createrv64imDisassembler(const Target &T, const MCSubtargetInfo &STI, MCContext &Ctx)
        {
            return new rv64imDisassembler(STI, Ctx, rv64imBaseInfo::IsBigEndian());
        }
                
        extern "C" void LLVMInitializerv64imDisassembler()
        {
            // Register Target Disassembler
            TargetRegistry::RegisterMCDisassembler(getTherv64imTarget(), createrv64imDisassembler);
        }
        """.trim().lines(), output);
  }
}
