package vadl.lcb.template.lib.Target.MCTargetDesc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Specification;

class EmitFixupKindsHeaderFilePassTest {
  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new vadl.lcb.lib.Target.MCTargetDesc.EmitFixupKindsHeaderFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        #ifndef LLVM_LIB_TARGET_specificationValue_specificationValueFIXUPKINDS_H
        #define LLVM_LIB_TARGET_specificationValue_specificationValueFIXUPKINDS_H
               
        #include "llvm/MC/MCFixup.h"
               
        namespace llvm
        {
            namespace specificationValue
            {
                enum Fixups
                {
                                    fixupKindIdentifierValue  = FirstTargetFixupKind,
                                    fixupKindIdentifierValue2 ,
                                    
                                 // Marker
                                 LastTargetFixupKind,
                             NumTargetFixupKinds = LastTargetFixupKind - FirstTargetFixupKind
                };
            }
        }
               
        #endif // LLVM_LIB_TARGET_specificationValue_specificationValueFIXUPKINDS_H
        """));
  }
}