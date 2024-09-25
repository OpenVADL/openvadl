# VADL generated file

gen_arch_ss = ss.source_set()
gen_arch_system_ss = ss.source_set()

gen_arch_ss.add(files(
               'translate.c',
               'cpu.c',
#               'disas.c'
))

gen_arch_system_ss.add(files(
  'machine.c'
))

target_arch += {'[(${gen_arch})]': gen_arch_ss}
target_system_arch += {'[(${gen_arch})]': gen_arch_system_ss}