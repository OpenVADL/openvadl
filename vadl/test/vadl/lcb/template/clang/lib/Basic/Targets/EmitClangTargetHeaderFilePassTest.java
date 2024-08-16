package vadl.lcb.template.clang.lib.Basic.Targets;

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

class EmitClangTargetHeaderFilePassTest {
  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new vadl.lcb.clang.lib.Basic.Targets.EmitClangTargetHeaderFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(null, specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        #ifndef LLVM_CLANG_LIB_BASIC_TARGETS_specificationValue_H
        #define LLVM_CLANG_LIB_BASIC_TARGETS_specificationValue_H
                
        #include "clang/Basic/TargetInfo.h"
        #include "clang/Basic/TargetOptions.h"
        #include "llvm/TargetParser/Triple.h"
        #include "llvm/Support/Compiler.h"
                
        namespace clang
        {
            namespace targets
            {
                class LLVM_LIBRARY_VISIBILITY specificationValueTargetInfo : public TargetInfo
                {
                
                    public:
                        specificationValueTargetInfo(const llvm::Triple &Triple, const TargetOptions &Opts) : TargetInfo(Triple)
                        {
                            SuitableAlign = 128; // TODO: FIXME: @chochrainer make this generic
                            WCharType = SignedInt;
                            WIntType = UnsignedInt;
                            IntPtrType = SignedInt;
                            PtrDiffType = SignedInt;
                            SizeType = UnsignedInt;
                            LongDoubleWidth = 128;
                            LongDoubleAlign = 128;
                            LongDoubleFormat = &llvm::APFloat::IEEEquad();
                            HasFloat16 = true;
                            HasStrictFP = true;
                            resetDataLayout("e-m:e-p:32:32-S0-a:0:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64");
                        }
                
                        void getTargetDefines(const LangOptions &Opts, MacroBuilder &Builder) const override { };
                
                        ArrayRef<Builtin::Info> getTargetBuiltins() const override { return ArrayRef<Builtin::Info>(); }
                
                        BuiltinVaListKind getBuiltinVaListKind() const override
                        {
                            return TargetInfo::VoidPtrBuiltinVaList;
                        }
                
                        std::string_view getClobbers() const override { return ""; }
                
                        ArrayRef<const char *> getGCCRegNames() const override;
                
                        ArrayRef<TargetInfo::GCCRegAlias> getGCCRegAliases() const override;
                
                        bool validateAsmConstraint(const char *&Name, TargetInfo::ConstraintInfo &Info) const override { return false; }
                };
            }
        }
                
        #endif
        """));
  }

}