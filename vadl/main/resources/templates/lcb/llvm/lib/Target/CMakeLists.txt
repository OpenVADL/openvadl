add_llvm_component_group([(${namespace})])
set(LLVM_TARGET_DEFINITIONS [(${namespace})].td)

tablegen(LLVM [(${namespace})]GenInstrInfo.inc -gen-instr-info)
tablegen(LLVM [(${namespace})]GenAsmWriter.inc -gen-asm-writer)
tablegen(LLVM [(${namespace})]GenCallingConv.inc -gen-callingconv)
tablegen(LLVM [(${namespace})]GenRegisterInfo.inc -gen-register-info)
tablegen(LLVM [(${namespace})]GenMCCodeEmitter.inc -gen-emitter)
tablegen(LLVM [(${namespace})]GenDAGISel.inc -gen-dag-isel)
tablegen(LLVM [(${namespace})]GenSubtargetInfo.inc -gen-subtarget)
tablegen(LLVM [(${namespace})]GenMCPseudoLowering.inc -gen-pseudo-lowering)
# tablegen(LLVM [(${namespace})]GenDisassemblerTables.inc -gen-disassembler)

add_public_tablegen_target([(${namespace})]CommonTableGen)

add_llvm_target([(${namespace})]CodeGen
          [(${namespace})]TargetMachine.cpp
          [(${namespace})]InstrInfo.cpp
          [(${namespace})]RegisterInfo.cpp
          [(${namespace})]SubTarget.cpp
          [(${namespace})]FrameLowering.cpp
          [(${namespace})]ISelLowering.cpp
          [(${namespace})]DAGToDAGISel.cpp
          [(${namespace})]AsmPrinter.cpp
          [(${namespace})]ExpandPseudo.cpp
          [(${namespace})]PassConfig.cpp
          [(${namespace})]TargetObjectFile.cpp
          [(${namespace})]MCInstLower.cpp

          LINK_COMPONENTS
          AsmPrinter
          CodeGen
          Core
          MC
          Support
          SelectionDAG
          [(${namespace})]Info
          [(${namespace})]Desc

          ADD_TO_COMPONENT
          [(${namespace})]
        )

add_subdirectory(AsmParser)
add_subdirectory(TargetInfo)
add_subdirectory(MCTargetDesc)
# add_subdirectory(Disassembler)