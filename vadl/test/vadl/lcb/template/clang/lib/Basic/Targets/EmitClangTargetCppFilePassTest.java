package vadl.lcb.template.clang.lib.Basic.Targets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.types.DataType;
import vadl.utils.SourceLocation;
import vadl.viam.Identifier;
import vadl.viam.Register;
import vadl.viam.Specification;

class EmitClangTargetCppFilePassTest {

  private static Register createRegister(String name) {
    return new Register(new Identifier(name, SourceLocation.INVALID_SOURCE_LOCATION),
        DataType.bits(32), Register.AccessKind.FULL,
        Register.AccessKind.FULL, null, new Register[] {});
  }

  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));
    specification.add(createRegister("registerValue1"));
    specification.add(createRegister("registerValue2"));
    specification.add(createRegister("registerValue3"));

    var template =
        new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetCppFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
           #include "specificationValue.h"
           #include "clang/Basic/MacroBuilder.h"
           #include "llvm/ADT/StringSwitch.h"
           
           using namespace clang;
           using namespace clang::targets;
           
           ArrayRef<const char *> specificationValueTargetInfo::getGCCRegNames() const
           {
               static const char *const GCCRegNames[] =
               {
               };
               return llvm::makeArrayRef( GCCRegNames );
           }
           
           ArrayRef<TargetInfo::GCCRegAlias> specificationValueTargetInfo::getGCCRegAliases() const
           {
               static const TargetInfo::GCCRegAlias GCCRegAliases[] =
               {
               };
               return llvm::makeArrayRef( GCCRegAliases );
           } 
        """));
  }


}