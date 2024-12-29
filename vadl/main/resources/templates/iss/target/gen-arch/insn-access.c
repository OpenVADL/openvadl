// [(${gen_arch_upper})] generated file

#include "insn-access.h"

#include <stdint.h>
#include "vadl-builtins.h"

[# th:if="${insn_access != null}"][# th:each="fn_def, iterState : ${insn_access}"][(${fn_def})]

[/][/]