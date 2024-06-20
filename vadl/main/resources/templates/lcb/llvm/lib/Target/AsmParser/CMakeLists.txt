add_llvm_component_library(LLVM[(${namespace})]AsmParser
          [(${namespace})]ParsedOperand.cpp
          [(${namespace})]AsmParser.cpp
          [(${namespace})]AsmRecursiveDescentParser.cpp

        LINK_COMPONENTS
        MC
        MCParser
        [(${namespace})]Desc
        [(${namespace})]Info
        Support

        ADD_TO_COMPONENT
        [(${namespace})]
        )