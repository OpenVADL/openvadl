# VADL generated file
gen = [
    decodetree.process('insn.decode', extra_args: [ '--decode', 'decode_insn' ])
]

gen_arch_ss = ss.source_set()
gen_arch_system_ss = ss.source_set()

gen_arch_ss.add(gen)
gen_arch_ss.add(files(
               'translate.c',
               'cpu.c',
#              'disas.c'
))

gen_arch_system_ss.add(files(
  'machine.c'
))

target_arch += {'[(${gen_arch})]': gen_arch_ss}
target_system_arch += {'[(${gen_arch})]': gen_arch_system_ss}