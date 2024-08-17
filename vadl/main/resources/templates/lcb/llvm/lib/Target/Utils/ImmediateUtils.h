#ifndef LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H
#define LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H

#include "llvm/Support/ErrorHandling.h"
#include <cstdint>
#include <unordered_map>
#include <vector>
#include <stdio.h>

// "__extension__" suppresses warning
__extension__ typedef          __int128 int128_t;
__extension__ typedef unsigned __int128 uint128_t;

[# th:each="function : ${decodeFunctions}" ]
[(${function})]
[/]

namespace
{
    class ImmediateUtils
    {
    public:
        // Enum to control which immediate functions to use.
        // Currently this is only used in the pseudo expansion pass.
        enum [(${namespace})]ImmediateKind{IK_UNKNOWN_IMMEDIATE // used for side effect registers which are interpreted as immediate
                    «FOR immediate : immediates», «immediate.loweredImmediate.immediateKindIdentifier»
                    «ENDFOR»};

        static uint64_t applyDecoding(const uint64_t value, [(${namespace})]ImmediateKind kind)
        {
            switch (kind)
            {
            default:
                llvm_unreachable("Unsupported immediate kind to use for decoding!");
            case IK_UNKNOWN_IMMEDIATE:
                return value;
            «FOR immediate : immediates» case «immediate.loweredImmediate.immediateKindIdentifier»:
                return «immediate.loweredImmediate.identifier»::«immediate.loweredImmediate.decoding.identifier»(value);
                «ENDFOR»
            }
        }
    };

} // end of anonymous namespace

#endif // LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H