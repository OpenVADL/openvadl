#ifndef LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H
#define LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H

#include "llvm/Support/ErrorHandling.h"
#include "vadl-builtins.h"
#include <cstdint>
#include <unordered_map>
#include <vector>
#include <stdio.h>
#include <bitset>

// "__extension__" suppresses warning
__extension__ typedef          __int128 int128_t;
__extension__ typedef unsigned __int128 uint128_t;

template<int start, int end, std::size_t N>
std::bitset<N> project_range(std::bitset<N> bits)
{
    std::bitset<N> result;
    size_t result_index = 0; // Index for the new bitset

    // Extract bits from the range [start, end]
    for (size_t i = start; i <= end; ++i) {
      result[result_index] = bits[i];
    result_index++;
    }

    return result;
}

[# th:each="function : ${decodeFunctions}" ]
static [(${function})]
[/]

[# th:each="function : ${encodeFunctions}" ]
static [(${function})]
[/]


[# th:each="function : ${predicateFunctions}" ]
static [(${function})]
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
                      , IK_[(${function})]
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
              case IK_[(${function})]:
                return [(${function})](value);
            [/th:block]
            }
        }
    };

} // end of anonymous namespace

#endif // LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H