#ifndef [(${gen_arch_upper})]_CPU_BITS_H
#define [(${gen_arch_upper})]_CPU_BITS_H

/* Exception causes */
typedef enum [(${gen_arch_upper})]Exception {
  [(${gen_arch_upper})]_EXCP_NONE = -1,
  [# th:each="exc, iterState : ${exc_info.exceptions}"]
  [[${exc.enum_name}]],
  [/]
} [(${gen_arch_upper})]Exception;

#endif //[(${gen_arch_upper})]_CPU_BITS_H
