package vadl.test.lcb.template;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.lcb.template.lib.Target.Utils.EmitImmediateFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class EmitImmediateFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var testSetup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(EmitImmediateFilePass.class.getName()));
    var passManager = testSetup.passManager();
    var spec = testSetup.specification();

    // When
    var template =
        new EmitImmediateFilePass(getConfiguration(false));

    // When
    var result = template.renderToString(passManager.getPassResults(), spec);
    var trimmed = result.trim();
    var output = trimmed.lines();

    Assertions.assertLinesMatch("""
        #ifndef LLVM_LIB_TARGET_rv64im_UTILS_IMMEDIATEUTILS_H
        #define LLVM_LIB_TARGET_rv64im_UTILS_IMMEDIATEUTILS_H
                
        #include "llvm/Support/ErrorHandling.h"
        #include <cstdint>
        #include <unordered_map>
        #include <vector>
        #include <stdio.h>
        #include <bitset>
                
        // "__extension__" suppresses warning
        __extension__ typedef          __int128 int128_t;
        __extension__ typedef unsigned __int128 uint128_t;
                
        template<int start, int end, std::size_t N>
        std::bitset<N> project_range(std::bitset<N> bits)
        {
            std::bitset<N> result;
            size_t result_index = 0; // Index for the new bitset
                
            // Extract bits from the range [start, end]
            for (size_t i = start; i <= end; ++i) {
              result[result_index] = bits[i];
            result_index++;
            }
                
            return result;
        }
                
                
        int64_t RV64IM_Btype_immS_decode(uint64_t param) {
        return (((int64_t) param)) << (1);
        }
        int64_t RV64IM_Itype_immS_decode(uint64_t param) {
        return ((int64_t) param);
        }
        int64_t RV64IM_Jtype_immS_decode(uint64_t param) {
        return (((int64_t) param)) << (1);
        }
        int64_t RV64IM_Stype_immS_decode(uint64_t param) {
        return ((int64_t) param);
        }
        uint64_t RV64IM_Utype_immU_decode(uint64_t param) {
        return (((uint64_t) param)) << (12);
        }
        uint8_t RV64IM_Ftype_shamt_decode(uint64_t param) {
        return param;
        }
        uint8_t RV64IM_Rtype_shamt_decode(uint64_t param) {
        return param;
        }
                
                
                
        uint16_t RV64IM_Btype_immS_encoding(int64_t immS) {
        return (project_range<1, 12>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        uint16_t RV64IM_Itype_immS_encoding(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        uint16_t RV64IM_Stype_immS_encoding(int64_t immS) {
        return (project_range<0, 11>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        uint32_t RV64IM_Jtype_immS_encoding(int64_t immS) {
        return (project_range<1, 20>(std::bitset<64>(immS)) << 0).to_ulong();
        }
        uint32_t RV64IM_Utype_immU_encoding(uint64_t immU) {
        return (project_range<12, 31>(std::bitset<64>(immU)) << 0).to_ulong();
        }
        uint8_t RV64IM_Ftype_shamt_encoding(uint8_t shamt) {
        return (project_range<0, 5>(std::bitset<6>(shamt)) << 0).to_ulong();
        }
        uint8_t RV64IM_Rtype_shamt_encoding(uint8_t shamt) {
        return (project_range<0, 4>(std::bitset<5>(shamt)) << 0).to_ulong();
        }
                
                
                
                
        bool RV64IM_Btype_immS_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV64IM_Ftype_shamt_predicate(uint8_t shamt_decode) {
        return 1;
        }
        bool RV64IM_Itype_immS_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV64IM_Jtype_immS_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV64IM_Rtype_shamt_predicate(uint8_t shamt_decode) {
        return 1;
        }
        bool RV64IM_Stype_immS_predicate(int64_t immS_decode) {
        return 1;
        }
        bool RV64IM_Utype_immU_predicate(uint64_t immU_decode) {
        return 1;
        }
                
                
        namespace
        {
            class ImmediateUtils
            {
            public:
                // Enum to control which immediate functions to use.
                // Currently this is only used in the pseudo expansion pass.
                enum rv64imImmediateKind{IK_UNKNOWN_IMMEDIATE // used for side effect registers which are interpreted as immediate
                             \s
                              , IK_RV64IM_Btype_immS_decode
                              , IK_RV64IM_Ftype_shamt_decode
                              , IK_RV64IM_Itype_immS_decode
                              , IK_RV64IM_Jtype_immS_decode
                              , IK_RV64IM_Rtype_shamt_decode
                              , IK_RV64IM_Stype_immS_decode
                              , IK_RV64IM_Utype_immU_decode
                             \s
                            };
                
                static uint64_t applyDecoding(const uint64_t value, rv64imImmediateKind kind)
                {
                    switch (kind)
                    {
                    default:
                        llvm_unreachable("Unsupported immediate kind to use for decoding!");
                    case IK_UNKNOWN_IMMEDIATE:
                        return value;
                   \s
                      case IK_RV64IM_Btype_immS_decode:
                        return RV64IM_Btype_immS_decode(value);
                      case IK_RV64IM_Ftype_shamt_decode:
                        return RV64IM_Ftype_shamt_decode(value);
                      case IK_RV64IM_Itype_immS_decode:
                        return RV64IM_Itype_immS_decode(value);
                      case IK_RV64IM_Jtype_immS_decode:
                        return RV64IM_Jtype_immS_decode(value);
                      case IK_RV64IM_Rtype_shamt_decode:
                        return RV64IM_Rtype_shamt_decode(value);
                      case IK_RV64IM_Stype_immS_decode:
                        return RV64IM_Stype_immS_decode(value);
                      case IK_RV64IM_Utype_immU_decode:
                        return RV64IM_Utype_immU_decode(value);
                   \s
                    }
                }
            };
                
        } // end of anonymous namespace
                
        #endif // LLVM_LIB_TARGET_rv64im_UTILS_IMMEDIATEUTILS_H
        """.trim().lines(), output);
  }
}
