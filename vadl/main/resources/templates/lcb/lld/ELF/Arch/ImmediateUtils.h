#ifndef LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H
#define LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H

#include <cstdint>

// collection of all available immediates

«FOR immediate : immediates»
    #include "«processorName»Immediates/«immediate.loweredImmediate.identifier».hpp"
«ENDFOR»

#endif // LLVM_LIB_TARGET_[(${namespace})]_UTILS_IMMEDIATEUTILS_H