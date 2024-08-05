package vadl.lcb.template.lib.Target.MCTargetDesc;

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

class EmitInstrPrinterCppFilePassTest {
  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new vadl.lcb.lib.Target.MCTargetDesc.EmitInstrPrinterCppFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(null, specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        #include "MCTargetDesc/specificationValueMCTargetDesc.h"
        #include "specificationValueInstPrinter.h"
        #include "Utils/ImmediateUtils.h"
        #include "llvm/MC/MCInst.h"
        #include "llvm/MC/MCExpr.h"
        #include "llvm/MC/MCSymbol.h"
        #include "llvm/Support/raw_ostream.h"
        #include "llvm/Support/Debug.h"
        #include "llvm/MC/MCSymbol.h"
        #include <string>
        #include <iostream>
        #include <sstream>
        #include <bitset>
        #include <iomanip>
                
        #define DEBUG_TYPE "specificationValueInstPrinter"
                
        using namespace llvm;
                
        #define GET_INSTRUCTION_NAME
        #include "specificationValueGenAsmWriter.inc"
        #undef GET_INSTRUCTION_NAME
                
        void specificationValueInstPrinter::anchor() {}
                
        void specificationValueInstPrinter::printRegName
            ( raw_ostream &O
            , MCRegister RegNo
            ) const
        {
            O << AsmUtils::getRegisterName( RegNo );
        }
                
        void specificationValueInstPrinter::printInst
            ( const MCInst *MI
            , uint64_t Address
            , StringRef Annot
            , const MCSubtargetInfo &STI
            , raw_ostream &O
            )
        {
            O << "\\t" << instToString(MI, Address);
            printAnnotation(O, Annot);
        }
                
        MCOperand specificationValueInstPrinter::adjustImmediateOp
            (const MCInst *MI, unsigned OpIndex) const
        {
            unsigned OpCode = MI->getOpcode();
            MCOperand original = MI->getOperand(OpIndex);
            int64_t value;
            if(AsmUtils::evaluateConstantImm(&original, value))
            {
                switch(OpCode)
                {
                    «FOR inst : processor.list( MachineInstruction )»
                        «IF inst.inputOperands( ImmediateOperand ).size == 1»
                            «IF inst.inputOperands( ImmediateOperand ).head.isEncodeBeforeEmit»
                                «emitAdjustImmediateOpFor( inst )»
                            «ENDIF»
                        «ENDIF»
                    «ENDFOR»
                }
            }
                
            return original;
        }
                
                
                
        std::string specificationValueInstPrinter::instToString(const MCInst *MI, uint64_t Address) const
        {
            switch ( MI->getOpcode() )
            {
                    case specificationValue::instructionValue:
                    {
                        // TODO
                        break;
                    }
            default:
                return "unknown instruction " + MI->getOpcode();
            }
        }
        """));
  }

}