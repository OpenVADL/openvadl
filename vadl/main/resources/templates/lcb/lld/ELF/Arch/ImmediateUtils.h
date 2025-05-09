#ifndef LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H
#define LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H

#include <cstdint>
#include "vadl-builtins.h"


// collection of all available immediates

[# th:each="function : ${encodeFunctions}" ]
static [(${function})]
[/]

#endif // LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H