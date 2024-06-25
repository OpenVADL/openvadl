#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]BASEINFO_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_[(${namespace})]BASEINFO_H

#include "llvm/Support/ErrorHandling.h"
#include <cstdint>

class [(${namespace})]BaseInfo
{
    public:

        // Enum for Machine Operand annotation
        enum
        {
            MO_None,
            «FOR relocation : relocations»
                «emitMOId( relocation )»,
            «ENDFOR»
            MO_Invalid
        };

        /* === Global Configs === */

        static bool IsBigEndian() { return «IF isBigEndian»true«ELSE»false«ENDIF»; }

        static int64_t applyRelocation( int64_t value, unsigned MOAnnotation )
        {
            switch( MOAnnotation )
            {
                default:
                    llvm_unreachable( "unsupported machine operand annotation relocation" );
                case MO_None:
                    return value;
                «FOR relocation : relocations»
                    case «relocation.moAnnotationIdentifier»:
                        return «relocation.oopFunction.identifier»( value );
                «ENDFOR»
            }
        }

        «IF relocations.size > 0»
            /* === Modifier Functions === */

            «FOR relocation : relocations»
                «emitRelocation( relocation )»

            «ENDFOR»
        «ENDIF»
};

#endif