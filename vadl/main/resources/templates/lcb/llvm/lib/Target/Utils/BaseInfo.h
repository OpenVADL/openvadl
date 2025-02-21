#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]BASEINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]BASEINFO_H

#include "llvm/Support/ErrorHandling.h"
#include "vadl-builtins.h"
#include <cstdint>

class [(${namespace})]BaseInfo
{
    public:

        // Enum for Machine Operand annotation
        enum
        {
            MO_None,
            [#th:block th:each="relocation : ${mos}" ]
            MO_[(${relocation.valueRelocationNameLower})],
            [/th:block]
            MO_Invalid
        };

        /* === Global Configs === */

        static bool IsBigEndian() {
          [#th:block th:if="${isBigEndian}"]return true;[/th:block]
          [#th:block th:if="${!isBigEndian}"]return false;[/th:block]
        }

        static int64_t applyRelocation( int64_t value, unsigned MOAnnotation )
        {
            switch( MOAnnotation )
            {
                default:
                    llvm_unreachable( "unsupported machine operand annotation relocation" );
                [#th:block th:each="relocation : ${mos}" ]
                case MO_[(${relocation.valueRelocationNameLower})]:
                    return [(${relocation.valueRelocationNameLower})](value);
                [/th:block]
                case MO_None:
                    return value;
            }
        }

        [# th:each="relocation : ${relocations}" ]
        [(${relocation.relocation.value})]
        [/]
};

#endif