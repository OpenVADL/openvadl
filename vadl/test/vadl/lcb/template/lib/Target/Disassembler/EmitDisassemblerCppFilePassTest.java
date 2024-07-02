package vadl.lcb.template.lib.Target.Disassembler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Specification;

class EmitDisassemblerCppFilePassTest {
  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new vadl.lcb.lib.Target.Disassembler.EmitDisassemblerCppFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        #include "specificationValueDisassembler.h"
        #include <iostream>
        #include "Utils/ImmediateUtils.h"
               
        #define DEBUG_TYPE "disassembler"
               
        using namespace llvm;
               
        specificationValueDisassembler::specificationValueDisassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian) : MCDisassembler(STI, Ctx), IsBigEndian(isBigEndian)
        {
        }
               
        /* == Register Classes == */
               
               
        /* == Immediate Decoding == */
               
        DecodeStatus decodeimmediateValue(MCInst &Inst, uint64_t Imm, int64_t Address, const void *Decoder)
        {
            Imm = Imm & 10 as long - 1;
            Imm = immediateValue::decodingValue;
            Inst.addOperand(MCOperand::createImm(Imm));
            return MCDisassembler::Success;
        }
               
               
               
               
        #include "specificationValueGenDisassemblerTables.inc"
               
            DecodeStatus specificationValueDisassembler::getInstruction(MCInst &MI, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CS) const
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
               
            auto Result = decodeInstruction(DecoderTable${instructionSize, MI, Instr, Address, this, STI);
            Size = 4;
            return Result;
        }
               
        static MCDisassembler *createspecificationValueDisassembler(const Target &T, const MCSubtargetInfo &STI, MCContext &Ctx)
        {
            return new specificationValueDisassembler(STI, Ctx, specificationValueBaseInfo::IsBigEndian());
        }
               
        extern "C" void LLVMInitializespecificationValueDisassembler()
        {
            // Register Target Disassembler
            TargetRegistry::RegisterMCDisassembler(getThespecificationValueTarget(), createspecificationValueDisassembler);
        }
        """));
  }
}