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

[# th:each="function : ${encodeFunctions}" ]
[(${function})]
[/]


[# th:each="function : ${predicateFunctions}" ]
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
                      [#th:block th:each="function, iterStat : ${decodeFunctionNames}" ]
                      , IK_[(${function.loweredName})]
                      [/th:block]
                    };

        static uint64_t applyDecoding(const uint64_t value, [(${namespace})]ImmediateKind kind)
        {
            switch (kind)
            {
            default:
                llvm_unreachable("Unsupported immediate kind to use for decoding!");
            case IK_UNKNOWN_IMMEDIATE:
                return value;
            [#th:block th:each="function, iterStat : ${decodeFunctionNames}" ]
              case IK_[(${function.loweredName})][#th:block th:if="${!iterStat.last}"]:[/th:block]
                return [(${function.functionName})](value);
            [/th:block]
            }
        }
    };

} // end of anonymous namespace

#endif // LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H