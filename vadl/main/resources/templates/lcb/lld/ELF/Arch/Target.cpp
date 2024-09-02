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
#include "[(${namespace})]ManualEncoding.hpp"
#include "[(${namespace})]Relocations.hpp"
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

            class [(${namespace})] final : public TargetInfo
            {
            public:
                RelExpr getRelExpr(RelType type, const Symbol &s,
                                   const uint8_t *loc) const override;
                void relocate(uint8_t * loc, const Relocation &rel, uint64_t val) const override;
            };

        } // end anonymous namespace

        RelExpr [(${namespace})]::getRelExpr(const RelType type, const Symbol &s,
                                            const uint8_t *loc) const
        {
            switch (type)
            {
            case R_[(${namespace})]_NONE:
                return R_NONE;
            case R_[(${namespace})]_32:
            case R_[(${namespace})]_64:
            [#th:block th:each="relocation: ${relocations}" ]
            case [(${relocation.name()})]:
                return [#th:block th:text="${relocation.kind() == 'ABSOLUTE'} ? 'R_ABS' : 'R_PC'" /];
            [/th:block]
            default:
                error(getErrorLocation(loc) + "unknown relocation (" + Twine(type) +
                                                     ") against symbol " + toString(s));
                return R_NONE;
            }
        }

        uint32_t read32(uint8_t *loc)
        {
            return read32[#th:block th:text="${isBigEndian}? 'be' : 'le'" \][/th:block](loc);
        }

        void write32(uint8_t *loc, uint32_t val)
        {
            write32[#th:block th:text="${isBigEndian}? 'be' : 'le'" \][/th:block](loc, val);
        }

        uint64_t read64(uint8_t *loc)
        {
            return read64[#th:block th:text="${isBigEndian}? 'be' : 'le'" \][/th:block](loc);
        }

        void write64(uint8_t *loc, uint64_t val)
        {
            write64[#th:block th:text="${isBigEndian}? 'be' : 'le'" \][/th:block](loc, val);
        }

        void [(${namespace})]::relocate(uint8_t *loc, const Relocation &rel, const uint64_t val) const
        {
            const unsigned bits = config->wordsize * 8;

            switch (rel.type)
            {
            case R_[(${namespace})]_32:
            {
                write32(loc, val);
                return;
            }

            case R_[(${namespace})]_64:
            {
                write64(loc, val);
                return;
            }
            default : llvm_unreachable("unknown relocation");
            }
        }

        TargetInfo *get[(${namespace})]TargetInfo()
        {
            static [(${namespace})] target;
            return &target;
        }

    } // namespace elf
} // namespace lld