#include "ABIInfoImpl.h"
#include "TargetInfo.h"

using namespace clang;
using namespace clang::CodeGen;

namespace
{
    class [(${namespace})]ABIInfo : public DefaultABIInfo
    {
    private:
        uint64_t RegisterBitSize = 32;

    public:
        [(${namespace})]ABIInfo(CodeGen::CodeGenTypes & CGT)
            : DefaultABIInfo(CGT) {}

        // DefaultABIInfo's classifyReturnType and classifyArgumentType are
        // non-virtual, but computeInfo is virtual, so we overload it.
        void computeInfo(CGFunctionInfo & FI) const override;
        ABIArgInfo extendType(QualType Ty) const;
        ABIArgInfo classifyArgumentType(QualType Ty, bool IsFixed, int &ArgGPRsLeft,
                                        int &ArgFPRsLeft) const;
        ABIArgInfo classifyReturnType(QualType RetTy) const;
    };
}

ABIArgInfo [(${namespace})]ABIInfo::extendType(QualType Ty) const
{
    int TySize = getContext().getTypeSize(Ty);
    // Here is more variation required. See https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/31 for details
    return ABIArgInfo::getExtend(Ty);
}

ABIArgInfo [(${namespace})]ABIInfo::classifyArgumentType(QualType Ty, bool IsFixed,
                                                        int &ArgGPRsLeft,
                                                        int &ArgFPRsLeft) const
{
    Ty = useFirstFieldIfTransparentUnion(Ty);

    // Ignore empty structs/unions.
    if (isEmptyRecord(getContext(), Ty, true))
        return ABIArgInfo::getIgnore();

    uint64_t Size = getContext().getTypeSize(Ty);
    if (!isAggregateTypeForABI(Ty) && !Ty->isVectorType())
    {
        // Treat an enum type as its underlying type.
        if (const EnumType *EnumTy = Ty->getAs<EnumType>())
            Ty = EnumTy->getDecl()->getIntegerType();

        if (Size < RegisterBitSize && Ty->isIntegralOrEnumerationType())
        {
            return extendType(Ty);
        }

        if (const auto *EIT = Ty->getAs<BitIntType>())
        {
            if (EIT->getNumBits() < RegisterBitSize)
                return extendType(Ty);
            if (EIT->getNumBits() > 128 ||
                (!getContext().getTargetInfo().hasInt128Type() &&
                 EIT->getNumBits() > 64))
                return getNaturalAlignIndirect(Ty, /*ByVal=*/false);
        }
    }

    return ABIArgInfo::getDirect();
}

ABIArgInfo [(${namespace})]ABIInfo::classifyReturnType(QualType RetTy) const
{
    if (RetTy->isVoidType())
        return ABIArgInfo::getIgnore();

    auto ArgGPRsLeft = 0;
    auto ArgFPRsLeft = 0;

    return classifyArgumentType(RetTy, /*IsFixed=*/true, ArgGPRsLeft, ArgFPRsLeft);
}

void [(${namespace})]ABIInfo::computeInfo(CGFunctionInfo &FI) const
{
    QualType RetTy = FI.getReturnType();
    FI.getReturnInfo() = classifyReturnType(RetTy);

    auto ArgGPRsLeft = 0;
    auto ArgFPRsLeft = 0;
    auto IsFixed = true;

    for (auto &ArgInfo : FI.arguments())
    {
        ArgInfo.info =
            classifyArgumentType(ArgInfo.type, IsFixed, ArgGPRsLeft, ArgFPRsLeft);
    }
}

namespace
{
    class [(${namespace})]TargetCodeGenInfo : public TargetCodeGenInfo{
        public :
            [(${namespace})]TargetCodeGenInfo(CodeGen::CodeGenTypes & CGT) : TargetCodeGenInfo(std::make_unique<[(${namespace})]ABIInfo>(CGT)){}
    };
} // namespace

std::unique_ptr<TargetCodeGenInfo>
CodeGen::create[(${namespace})]TargetCodeGenInfo(CodeGenModule &CGM)
{
    return std::make_unique<[(${namespace})]TargetCodeGenInfo>(CGM.getTypes());
}