package vadl.lcb.template.lld.ELF.Arch;

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

class EmitLldArchFilePassTest {

  private static LcbConfiguration createLcbConfiguration() {
    return new LcbConfiguration("");
  }

  @Test
  void shouldRenderTemplate() throws IOException {
    // Given
    var specification = new Specification(
        new Identifier("specificationValue", SourceLocation.INVALID_SOURCE_LOCATION));

    var template =
        new vadl.lcb.lld.ELF.Arch.EmitLldArchFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(specification, writer);
    var output = writer.toString();

    // Then
    assertThat(output, equalToIgnoringWhiteSpace("""
        //===----------------------------------------------------------------------===//
        //
        // Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
        // See https://llvm.org/LICENSE.txt for license information.
        // SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
        //
        //===----------------------------------------------------------------------===//
               
        #include "InputFiles.h"
        #include "Symbols.h"
        #include "SyntheticSections.h"
        #include "Target.h"
        #include "specificationValueManualEncoding.hpp"
        #include "specificationValueRelocations.hpp"
        #include "ImmediateUtils.h"
               
        using namespace llvm;
        using namespace llvm::object;
        using namespace llvm::support::endian;
        using namespace llvm::ELF;
               
        namespace lld
        {
            namespace elf
            {
               
                namespace
                {
               
                    class specificationValue final : public TargetInfo
                    {
                    public:
                        RelExpr getRelExpr(RelType type, const Symbol &s,
                                           const uint8_t *loc) const override;
                        void relocate(uint8_t * loc, const Relocation &rel, uint64_t val) const override;
                    };
               
                } // end anonymous namespace
               
                RelExpr specificationValue::getRelExpr(const RelType type, const Symbol &s,
                                                    const uint8_t *loc) const
                {
                    switch (type)
                    {
                    case R_specificationValue_NONE:
                        return R_NONE;
                    case R_specificationValue_32:
                    case R_specificationValue_64:
                    case identifierValue:
                        return R_ABS;
                    default:
                        error(getErrorLocation(loc) + "unknown relocation (" + Twine(type) +
                                                             ") against symbol " + toString(s));
                        return R_NONE;
                    }
                }
               
                uint32_t read32(uint8_t *loc)
                {
                    return read32le(loc);
                }
               
                void write32(uint8_t *loc, uint32_t val)
                {
                    write32le(loc, val);
                }
               
                uint64_t read64(uint8_t *loc)
                {
                    return read64le(loc);
                }
               
                void write64(uint8_t *loc, uint64_t val)
                {
                    write64le(loc, val);
                }
               
                void specificationValue::relocate(uint8_t *loc, const Relocation &rel, const uint64_t val) const
                {
                    const unsigned bits = config->wordsize * 8;
               
                    switch (rel.type)
                    {
                    case R_specificationValue_32:
                    {
                        write32(loc, val);
                        return;
                    }
               
                    case R_specificationValue_64:
                    {
                        write64(loc, val);
                        return;
                    }
                    default : llvm_unreachable("unknown relocation");
                    }
                }
               
                TargetInfo *getspecificationValueTargetInfo()
                {
                    static specificationValue target;
                    return &target;
                }
               
            } // namespace elf
        } // namespace lld 
        """));
  }

}