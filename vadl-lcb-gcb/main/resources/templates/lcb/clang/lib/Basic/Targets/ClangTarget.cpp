#include "[(${namespace})].h"
#include "clang/Basic/MacroBuilder.h"
#include "clang/Basic/TargetBuiltins.h"
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

static constexpr Builtin::Info BuiltinInfo[] = {
#define BUILTIN(ID, TYPE, ATTRS)                                               \
  {#ID, TYPE, ATTRS, nullptr, HeaderDesc::NO_HEADER, ALL_LANGUAGES},
#define TARGET_BUILTIN(ID, TYPE, ATTRS, FEATURE)                               \
  {#ID, TYPE, ATTRS, FEATURE, HeaderDesc::NO_HEADER, ALL_LANGUAGES},
#include "clang/Basic/Builtins[(${namespace})].inc"
};

ArrayRef<Builtin::Info> [(${namespace})]TargetInfo::getTargetBuiltins() const {
  return llvm::ArrayRef(BuiltinInfo,
                        clang::[(${namespace})]::LastTSBuiltin - Builtin::FirstTSBuiltin);
}