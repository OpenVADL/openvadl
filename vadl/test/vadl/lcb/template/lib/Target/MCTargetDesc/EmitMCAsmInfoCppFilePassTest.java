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

class EmitMCAsmInfoCppFilePassTest {

  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new vadl.lcb.lib.Target.MCTargetDesc.EmitMCAsmInfoCppFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(null, specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        #include "specificationValueMCAsmInfo.h"
        #include "llvm/TargetParser/Triple.h"
                
        using namespace llvm;
                
        void specificationValueMCAsmInfo::anchor() {}
                
        specificationValueMCAsmInfo::specificationValueMCAsmInfo(const Triple &TheTriple)
        {
            CommentString = "commentValue";
            AlignmentIsInBytes = 16;
        }
        """));
  }

}