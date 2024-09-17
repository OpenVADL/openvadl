add_llvm_component_library(LLVM[(${namespace})]AsmParser
          AsmParsedOperand.cpp
          [(${namespace})]AsmParser.cpp
          AsmRecursiveDescentParser.cpp

        LINK_COMPONENTS
        MC
        MCParser
        [(${namespace})]Desc
        [(${namespace})]Info
        Support

        ADD_TO_COMPONENT
        [(${namespace})]
        )