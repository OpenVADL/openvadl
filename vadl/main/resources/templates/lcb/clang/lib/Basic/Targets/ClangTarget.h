#ifndef LLVM_CLANG_LIB_BASIC_TARGETS_[(${namespace})]_H
#define LLVM_CLANG_LIB_BASIC_TARGETS_[(${namespace})]_H

#include "clang/Basic/TargetInfo.h"
#include "clang/Basic/TargetOptions.h"
#include "llvm/TargetParser/Triple.h"
#include "llvm/Support/Compiler.h"

namespace clang
{
    namespace targets
    {
        class LLVM_LIBRARY_VISIBILITY [(${namespace})]TargetInfo : public TargetInfo
        {

            public:
                [(${namespace})]TargetInfo(const llvm::Triple &Triple, const TargetOptions &Opts) : TargetInfo(Triple)
                {
                    SuitableAlign = 128; // TODO: FIXME: @chochrainer make this generic
                    WCharType = SignedInt;
                    WIntType = UnsignedInt;
                    IntPtrType = SignedInt;
                    PtrDiffType = SignedInt;

                    [# th:each="ty : ${clangTypes}" ]
                    [(${ty.name})] = [(${ty.value})];
                    [/]

                    LongDoubleWidth = 128;
                    LongDoubleAlign = 128;
                    LongDoubleFormat = &llvm::APFloat::IEEEquad();
                    HasFloat16 = true;
                    HasStrictFP = true;
                    resetDataLayout("[(${datalayout})]");
                }

                ArrayRef<Builtin::Info> getTargetBuiltins() const override { return ArrayRef<Builtin::Info>(); }

                BuiltinVaListKind getBuiltinVaListKind() const override
                {
                    return TargetInfo::VoidPtrBuiltinVaList;
                }

                std::string_view getClobbers() const override { return ""; }

                ArrayRef<const char *> getGCCRegNames() const override;

                ArrayRef<TargetInfo::GCCRegAlias> getGCCRegAliases() const override;

                bool validateAsmConstraint(const char *&Name, TargetInfo::ConstraintInfo &Info) const override { return false; }

                void getTargetDefines(const LangOptions &Opts,MacroBuilder &Builder) const;
        };
    }
}

#endif