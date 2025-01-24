#include "llvm/ADT/SmallVector.h"
#include "llvm/MC/MCRegister.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include <cstdint>
#include <bitset>
#include "rv32imConstMatInt.h"
#include "rv32imDAGToDAGISel.h"
#include "rv32imRegisterInfo.h"
#include "rv32imSubTarget.h"

using namespace llvm;

namespace llvm::[(${namespace})]MatInt {
  InstSeq generateInstSeqImpl(int64_t Val, [(${namespace})]MatInt::InstSeq &Res ) {
    if (isInt<32>(Val)) {
        // Depending on the active bits in the immediate Value v, the following
        // instruction sequences are emitted:
        //
        // v == 0                        : ADDI
        // v[0,12) != 0 && v[12,32) == 0 : ADDI
        // v[0,12) == 0 && v[12,32) != 0 : LUI
        // v[0,32) != 0                  : LUI+ADDI(W)
        auto Hi20 = [(${luiRawEncoderMethod})]([(${largestPossibleValueAddi})] + 1 + Val);
        int64_t Lo12 = SignExtend64<[(${addiBitSize})]>(Val);

        if (Hi20)
          Res.emplace_back([(${namespace})]::[(${lui})], Hi20);

        if (Lo12 || Hi20 == 0) {
          unsigned AddiOpc = [(${namespace})]::[(${addi})];
          Res.emplace_back(AddiOpc, Lo12);
        }
        return Res;
      }

      int64_t Lo12 = SignExtend64<[(${addiBitSize})]>(Val);
      Val = (uint64_t)Val - (uint64_t)Lo12;

      int ShiftAmount = 0;
      bool Unsigned = false;

      // Val might now be valid for LUI without needing a shift.
      if (!isInt<32>(Val)) {
        ShiftAmount = llvm::countr_zero((uint64_t)Val);
        Val >>= ShiftAmount;

        // If the remaining bits don't fit in 12 bits, we might be able to reduce
        // the // shift amount in order to use LUI which will zero the lower 12
        // bits.
        if (ShiftAmount > [(${addiBitSize})] && !isInt<[(${addiBitSize})]>(Val)) {
          if (isInt<32>((uint64_t)Val << [(${addiBitSize})])) {
            // Reduce the shift amount and add zeros to the LSBs so it will match
            // LUI.
            ShiftAmount -= [(${addiBitSize})];
            Val = (uint64_t)Val << [(${addiBitSize})];
          }
        }
      }

      generateInstSeqImpl(Val, Res);

      // Skip shift if we were able to use LUI directly.
      if (ShiftAmount) {
        unsigned Opc = [(${namespace})]::[(${slli})];
        Res.emplace_back(Opc, ShiftAmount);
      }

      if (Lo12) {
        Res.emplace_back([(${namespace})]::[(${addi})], Lo12);
      }

    return Res;
  }

  InstSeq generateInstSeq(int64_t Val) {
    [(${namespace})]MatInt::InstSeq Res;
    generateInstSeqImpl(Val, Res);
    return Res;
  }
}