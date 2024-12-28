// [(${gen_arch_upper})] generated file

// TODO: import built-in functions
#include "insn-access.h"

[# th:if="${insn_access != null}"][# th:each="fn_def, iterState : ${insn_access}"][(${fn_def})]

[/][/]