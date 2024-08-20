package vadl.test.lcb.template;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.passes.field_node_replacement.FieldNodeReplacementPassForDecoding;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.test.lcb.AbstractLcbTest;
import vadl.viam.passes.typeCastElimination.TypeCastEliminationPass;

public class EmitImmediateFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var passManager = new PassManager();
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");

    passManager.add(new PassKey("ty"), new TypeCastEliminationPass());
    passManager.add(new PassKey("encoding"), new GenerateFieldAccessEncodingFunctionPass());
    passManager.add(new PassKey("fieldDecoding"), new FieldNodeReplacementPassForDecoding());
    passManager.add(new PassKey(CppTypeNormalizationForPredicatesPass.class.getName()),
        new CppTypeNormalizationForPredicatesPass());
    passManager.add(new PassKey(CppTypeNormalizationForDecodingsPass.class.getName()),
        new CppTypeNormalizationForDecodingsPass());
    passManager.add(new PassKey(CppTypeNormalizationForEncodingsPass.class.getName()),
        new CppTypeNormalizationForEncodingsPass());

    passManager.run(spec);

    // When
    var template =
        new EmitImmediateFilePass(createLcbConfiguration(),
            new ProcessorName("processorNameValue"));
    var writer = new StringWriter();

    // When
    template.renderToString(passManager.getPassResults(), spec, writer);
    var trimmed = writer.toString().trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_rv3264im_UTILS_IMMEDIATEUTILS_H
        #define LLVM_LIB_TARGET_rv3264im_UTILS_IMMEDIATEUTILS_H

        #include "llvm/Support/ErrorHandling.h"
        #include <cstdint>
        #include <unordered_map>
        #include <vector>
        #include <stdio.h>

        // "__extension__" suppresses warning
        __extension__ typedef          __int128 int128_t;
        __extension__ typedef unsigned __int128 uint128_t;


        int64_t RV3264I_Btype_immS_decode_decode(uint16_t param) {
        return (((int64_t) param)) << (1);
        }
        int64_t RV3264I_Stype_immS_decode_decode(uint16_t param) {
        return ((int64_t) param);
        }
        int64_t RV3264I_Itype_immS_decode_decode(uint16_t param) {
        return ((int64_t) param);
        }
        int64_t RV3264I_Jtype_immS_decode_decode(uint32_t param) {
        return (((int64_t) param)) << (1);
        }
        uint64_t RV3264I_Utype_immU_decode_decode(uint32_t param) {
        return (((uint64_t) param)) << (12);
        }
        uint8_t RV3264I_Ftype_shamt_decode_decode(uint8_t param) {
        return param;
        }
        uint8_t RV3264I_Rtype_shamt_decode_decode(uint8_t param) {
        return param;
        }



        uint8_t RV3264I_Ftype_shamt_encoding_encode(uint8_t shamt) {
        return (((shamt) & ((1U << 6) - 1)) >> 0);
        }
        uint16_t RV3264I_Btype_immS_encoding_encode(int64_t immS) {
        return (((immS) & ((1U << 13) - 1) & ~((1 << 1) - 1)) >> 1);
        }
        uint16_t RV3264I_Stype_immS_encoding_encode(int64_t immS) {
        return (((immS) & ((1U << 12) - 1)) >> 0);
        }
        uint8_t RV3264I_Rtype_shamt_encoding_encode(uint8_t shamt) {
        return (((shamt) & ((1U << 5) - 1)) >> 0);
        }
        uint16_t RV3264I_Itype_immS_encoding_encode(int64_t immS) {
        return (((immS) & ((1U << 12) - 1)) >> 0);
        }
        uint32_t RV3264I_Utype_immU_encoding_encode(uint64_t immU) {
        return (((immU) & ((1U << 32) - 1) & ~((1 << 12) - 1)) >> 12);
        }
        uint32_t RV3264I_Jtype_immS_encoding_encode(int64_t immS) {
        return (((immS) & ((1U << 21) - 1) & ~((1 << 1) - 1)) >> 1);
        }




        bool RV3264I_Btype_immS_predicate_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV3264I_Stype_immS_predicate_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV3264I_Itype_immS_predicate_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV3264I_Jtype_immS_predicate_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV3264I_Utype_immU_predicate_predicate(uint64_t immU_decode) {
        return 1;
        }
        bool RV3264I_Ftype_shamt_predicate_predicate(uint8_t shamt_decode) {
        return 1;
        }
        bool RV3264I_Rtype_shamt_predicate_predicate(uint8_t shamt_decode) {
        return 1;
        }


        namespace
        {
            class ImmediateUtils
            {
            public:
                // Enum to control which immediate functions to use.
                // Currently this is only used in the pseudo expansion pass.
                enum rv3264imImmediateKind{IK_UNKNOWN_IMMEDIATE // used for side effect registers which are interpreted as immediate
                             \s
                              , IK_RV3264I_Btype_immS_decode
                              , IK_RV3264I_Stype_immS_decode
                              , IK_RV3264I_Itype_immS_decode
                              , IK_RV3264I_Jtype_immS_decode
                              , IK_RV3264I_Utype_immU_decode
                              , IK_RV3264I_Ftype_shamt_decode
                              , IK_RV3264I_Rtype_shamt_decode
                             \s
                            };

                static uint64_t applyDecoding(const uint64_t value, rv3264imImmediateKind kind)
                {
                    switch (kind)
                    {
                    default:
                        llvm_unreachable("Unsupported immediate kind to use for decoding!");
                    case IK_UNKNOWN_IMMEDIATE:
                        return value;
                   \s
                      case IK_RV3264I_Btype_immS_decode:
                        return RV3264I_Btype_immS_decode_decode(value);
                      case IK_RV3264I_Stype_immS_decode:
                        return RV3264I_Stype_immS_decode_decode(value);
                      case IK_RV3264I_Itype_immS_decode:
                        return RV3264I_Itype_immS_decode_decode(value);
                      case IK_RV3264I_Jtype_immS_decode:
                        return RV3264I_Jtype_immS_decode_decode(value);
                      case IK_RV3264I_Utype_immU_decode:
                        return RV3264I_Utype_immU_decode_decode(value);
                      case IK_RV3264I_Ftype_shamt_decode:
                        return RV3264I_Ftype_shamt_decode_decode(value);
                      case IK_RV3264I_Rtype_shamt_decode:
                        return RV3264I_Rtype_shamt_decode_decode(value);
                   \s
                    }
                }
            };

        } // end of anonymous namespace

        #endif // LLVM_LIB_TARGET_rv3264im_UTILS_IMMEDIATEUTILS_H 
        """.trim().lines(), output);
  }
}
