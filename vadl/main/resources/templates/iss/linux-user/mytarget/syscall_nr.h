#ifndef [(${gen_arch_upper})]_SYSCALL_NR_H
#define [(${gen_arch_upper})]_SYSCALL_NR_H

[# th:each="mapping : ${config.syscallMappings}"]
#define TARGET_NR_[(${mapping.key})] [(${mapping.value})]
[/]

// maximum syscall number limit to size internal syscall tables
#define TARGET_NR_syscalls 450

#endif