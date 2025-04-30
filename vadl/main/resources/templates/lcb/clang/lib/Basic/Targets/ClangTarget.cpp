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
    return llvm::makeArrayRef( GCCRegNames );
}

ArrayRef<TargetInfo::GCCRegAlias> [(${namespace})]TargetInfo::getGCCRegAliases() const
{
    static const TargetInfo::GCCRegAlias GCCRegAliases[] =
    {
    [# th:each="register, iterStat : ${registers}" ]
        {
            {[# th:each="alias, iterAliasStat : ${registers}" ] "[(${alias.name})]"[# th:if="${!iterAliasStat.last}" ],[/] [/]},
            "[(${register.name})]"
        }[# th:if="${!iterStat.last}" ],[/]
    [/]
    };
    return llvm::makeArrayRef( GCCRegAliases );
}

void [(${namespace})]TargetInfo::getTargetDefines(const LangOptions &Opts,
		MacroBuilder &Builder) const {
	Builder.defineMacro("__ELF__");
	Builder.defineMacro("__riscv");
	Builder.defineMacro("__riscv_cmodel_medlow");
}