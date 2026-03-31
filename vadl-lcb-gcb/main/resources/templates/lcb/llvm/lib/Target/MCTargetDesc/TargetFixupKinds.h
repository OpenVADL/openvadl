#ifndef LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]FIXUPKINDS_H
#define LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]FIXUPKINDS_H

#include "llvm/MC/MCFixup.h"

namespace llvm
{
    namespace [(${namespace})]
    {
        enum Fixups
        {

                    [#th:block th:if="${fixups.size > 0}" ]
                        [#th:block th:each="fixup, iterStat : ${fixups}" ]
                            [(${fixup.name})] [#th:block th:if="${iterStat.first}"] = FirstTargetFixupKind[/th:block],
                        [/th:block]

                         // Marker
                         LastTargetFixupKind,
                     NumTargetFixupKinds = LastTargetFixupKind - FirstTargetFixupKind
                    [/th:block]
                    [#th:block th:if="${fixups.size == 0}" ]
                        // Marker
                        LastTargetFixupKind = FirstTargetFixupKind,
                        NumTargetFixupKinds = LastTargetFixupKind - FirstTargetFixupKind
                    [/th:block]
        };
    }
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_[(${namespace})]FIXUPKINDS_H