add_llvm_component_library(LLVM[(${namespace})]Desc
          [(${namespace})]MCTargetDesc.cpp
          [(${namespace})]MCAsmInfo.cpp
          [(${namespace})]InstPrinter.cpp
          [(${namespace})]MCExpr.cpp
          [(${namespace})]AsmBackend.cpp
          [(${namespace})]ELFStreamer.cpp
          [(${namespace})]AsmStreamer.cpp
          [(${namespace})]ELFObjectWriter.cpp
          [(${namespace})]MCCodeEmitter.cpp
          [(${namespace})]MCInstExpander.cpp
          AsmUtils.cpp

          LINK_COMPONENTS
          MC
          Support
          [(${namespace})]Info

          ADD_TO_COMPONENT
          [(${namespace})]
        )
