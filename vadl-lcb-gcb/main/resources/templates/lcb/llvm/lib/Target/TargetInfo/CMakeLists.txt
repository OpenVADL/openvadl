add_llvm_component_library(LLVM[(${namespace})]Info
            [(${namespace})]TargetInfo.cpp

            LINK_COMPONENTS
            Support

            ADD_TO_COMPONENT
            [(${namespace})]
        )
