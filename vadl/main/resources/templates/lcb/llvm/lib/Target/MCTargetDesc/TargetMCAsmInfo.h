#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCASMINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCASMINFO_H

#include "llvm/MC/MCAsmInfoELF.h"

namespace llvm
{
    class Triple;

    class [(${namespace})]MCAsmInfo : public MCAsmInfoELF
    {
        void anchor() override;

        public:
            explicit [(${namespace})]MCAsmInfo(const Triple &TheTriple);
    };
}

#endif // LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]MCASMINFO_H