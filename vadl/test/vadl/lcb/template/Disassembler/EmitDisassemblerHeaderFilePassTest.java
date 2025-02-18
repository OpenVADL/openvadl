package vadl.lcb.template.Disassembler;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.Disassembler.EmitDisassemblerHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.lcb.AbstractLcbTest;

public class EmitDisassemblerHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitDisassemblerHeaderFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitDisassemblerHeaderFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_processornamevalue_DISASSEMBLER_processornamevalueDISASSEMBLER_H
        #define LLVM_LIB_TARGET_processornamevalue_DISASSEMBLER_processornamevalueDISASSEMBLER_H
                
        #include "MCTargetDesc/processornamevalueMCTargetDesc.h"
        #include "TargetInfo/processornamevalueTargetInfo.h"
        #include "Utils/processornamevalueBaseInfo.h"
        #include "llvm/CodeGen/Register.h"
        #include "llvm/MC/MCContext.h"
        #include "llvm/MC/MCDisassembler/MCDisassembler.h"
        #include "llvm/MC/MCDecoderOps.h"
        #include "llvm/MC/MCInst.h"
        #include "llvm/MC/MCRegisterInfo.h"
        #include "llvm/MC/MCSubtargetInfo.h"
        #include "llvm/Support/Endian.h"
        #include "llvm/MC/TargetRegistry.h"
                
        using namespace llvm;
                
        typedef MCDisassembler::DecodeStatus DecodeStatus;
                
        namespace llvm
        {
            class processornamevalueDisassembler : public MCDisassembler
            {
            public:
                processornamevalueDisassembler(const MCSubtargetInfo &STI, MCContext &Ctx, bool isBigEndian);
                
                DecodeStatus getInstruction(MCInst &Instr, uint64_t &Size, ArrayRef<uint8_t> Bytes, uint64_t Address, raw_ostream &CStream) const override;
                
            protected:
                bool IsBigEndian;
            };
        } // end llvm namespace
                
        #endif 
        """.trim().lines(), output);
  }
}
