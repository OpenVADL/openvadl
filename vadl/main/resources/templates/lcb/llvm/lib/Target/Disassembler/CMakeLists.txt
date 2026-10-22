add_llvm_component_library(LLVM[(${namespace})]Disassembler
  [(${namespace})]Disassembler.cpp

  LINK_COMPONENTS
  MC
  MCDisassembler
  [(${namespace})]Desc
  [(${namespace})]Info
  Support

  ADD_TO_COMPONENT
  [(${namespace})]
  )