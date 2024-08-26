package vadl.test.lcb.template;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.lcb.template.lib.Target.EmitInstrInfoTableGenFilePass;
import vadl.lcb.template.lib.Target.EmitRegisterInfoHeaderFilePass;
import vadl.pass.PassKey;
import vadl.pass.StringOutputFactory;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class EmitRegisterInfoHeaderFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfigurationWithStringWriter(false);
    var testSetup = runLcb(configuration, "examples/rv3264im.vadl",
        new PassKey(EmitRegisterInfoHeaderFilePass.class.getName()));

    // When
    var writer = ((StringOutputFactory) configuration.outputFactory()).getLastStringWriter();

    // Then
    var trimmed = writer.toString().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_rv3264im_rv3264imREGISTERINFO_H
        #define LLVM_LIB_TARGET_rv3264im_rv3264imREGISTERINFO_H
               
        #include "llvm/CodeGen/TargetRegisterInfo.h"
        #include <string>
               
        #define GET_REGINFO_HEADER
        #include "rv3264imGenRegisterInfo.inc"
               
        namespace llvm
        {
            struct rv3264imRegisterInfo : public rv3264imGenRegisterInfo
            {
                // virtual anchor method to decrease link time as the vtable
                virtual void anchor();
               
                rv3264imRegisterInfo();
               
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
               
        #endif // LLVM_LIB_TARGET_rv3264im_rv3264imREGISTERINFO_H
                """.trim().lines(), output);
  }
}
