#include "[(${namespace})].h"
#include "clang/Basic/MacroBuilder.h"
#include "llvm/ADT/StringSwitch.h"

using namespace clang;
using namespace clang::targets;

ArrayRef<const char *> [(${namespace})]TargetInfo::getGCCRegNames() const
{
    static const char *const GCCRegNames[] =
    {
        [#th:block th:each="register, iterStat : ${registers}" ]
            "[(${register.name})]"[#th:block th:if="${!iterStat.last}"],[/th:block]
        [/th:block]
    };
    return llvm::ArrayRef( GCCRegNames );
}

ArrayRef<TargetInfo::GCCRegAlias> [(${namespace})]TargetInfo::getGCCRegAliases() const
{
    std::vector<TargetInfo::GCCRegAlias> aliases; // keep it empty
    return llvm::ArrayRef(aliases);
}

void [(${namespace})]TargetInfo::getTargetDefines(const LangOptions &Opts,
		MacroBuilder &Builder) const {
	Builder.defineMacro("__ELF__");
	Builder.defineMacro("__riscv");
	Builder.defineMacro("__riscv_cmodel_medany");
}