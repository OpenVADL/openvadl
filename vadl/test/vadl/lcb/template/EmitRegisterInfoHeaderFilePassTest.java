package vadl.lcb.template;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.io.Files;
import vadl.lcb.template.lib.Target.EmitRegisterInfoHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.lcb.AbstractLcbTest;

public class EmitRegisterInfoHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    var testSetup = runLcb(configuration, "sys/risc-v/rv64im.vadl",
        new PassKey(EmitRegisterInfoHeaderFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitRegisterInfoHeaderFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var trimmed = Files.asCharSource(resultFile, Charset.defaultCharset()).read().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_processornamevalue_processornamevalueREGISTERINFO_H
        #define LLVM_LIB_TARGET_processornamevalue_processornamevalueREGISTERINFO_H
                
        #include "llvm/CodeGen/TargetRegisterInfo.h"
        #include <string>
                
        #define GET_REGINFO_HEADER
        #include "processornamevalueGenRegisterInfo.inc"
                
        namespace llvm
        {
            struct processornamevalueRegisterInfo : public processornamevalueGenRegisterInfo
            {
                // virtual anchor method to decrease link time as the vtable
                virtual void anchor();
                
                processornamevalueRegisterInfo();
                
                const uint32_t *getCallPreservedMask(const MachineFunction &MF, CallingConv::ID) const override;
                
                const uint16_t *getCalleeSavedRegs(const MachineFunction *MF = nullptr) const override;
                
                BitVector getReservedRegs(const MachineFunction &MF) const override;
                
                bool requiresRegisterScavenging(const MachineFunction &MF) const override
                {
                    return true;
                }
                
                bool requiresFrameIndexScavenging(const MachineFunction &MF) const override
                {
                    return true;
                }
                
                bool trackLivenessAfterRegAlloc(const MachineFunction &) const override
                {
                    return true;
                }
                
                bool useFPForScavengingIndex(const MachineFunction &MF) const override
                {
                    return false;
                }
                
                bool eliminateFrameIndex(MachineBasicBlock::iterator II, int SPAdj,
                                         unsigned FIOperandNum,
                                         RegScavenger *RS = nullptr) const override;
                
               \s
                static unsigned X(unsigned index);
               \s
                
                static unsigned registerOpcodeLookup(std::string className, unsigned index);
                Register getFrameRegister(const MachineFunction &MF) const override;
            };
        } // end namespace llvm
                
        #endif // LLVM_LIB_TARGET_processornamevalue_processornamevalueREGISTERINFO_H
        """.trim().lines(), output);
  }
}
