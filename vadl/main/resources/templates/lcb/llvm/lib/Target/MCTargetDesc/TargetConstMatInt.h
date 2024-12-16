//===- [(${namespace})]MatInt.h - Immediate materialisation ---------------*- C++ -*--===//
//
// Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
// See https://llvm.org/LICENSE.txt for license information.
// SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
//
//===----------------------------------------------------------------------===//

#ifndef LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_MATINT_H
#define LLVM_LIB_TARGET_[(${namespace})]_MCTARGETDESC_MATINT_H

#include "llvm/ADT/SmallVector.h"
#include "llvm/MC/MCRegister.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include <cstdint>
#include <bitset>

namespace llvm {
class APInt;

namespace [(${namespace})]MatInt {
  class Inst {
    unsigned Opc;
    int32_t Imm; // The largest value we need to store is 20 bits.

  public:
    Inst(unsigned Opc, int64_t I) : Opc(Opc), Imm(I) {
      assert(I == Imm && "truncated");
    }

    unsigned getOpcode() const { return Opc; }
    int64_t getImm() const { return Imm; }
  };

  using InstSeq = SmallVector<Inst, 8>;

  // Helper to generate an instruction sequence that will materialise the given
  // immediate value into a register. A sequence of instructions represented by a
  // simple struct is produced rather than directly emitting the instructions in
  // order to allow this helper to be used from both the MC layer and during
  // instruction selection.
  InstSeq generateInstSeq(int64_t Val, const MCSubtargetInfo &STI);
  InstSeq generateInstSeqImpl(int64_t Val, const MCSubtargetInfo &STI, InstSeq &Res);
}
}

namespace llvm::[(${namespace})]MatInt {
  InstSeq generateInstSeqImpl(int64_t Val, const MCSubtargetInfo &STI, [(${namespace})]MatInt::InstSeq &Res ) {
    if (isInt<32>(Val)) {
        // Depending on the active bits in the immediate Value v, the following
        // instruction sequences are emitted:
        //
        // v == 0                        : ADDI
        // v[0,12) != 0 && v[12,32) == 0 : ADDI
        // v[0,12) == 0 && v[12,32) != 0 : LUI
        // v[0,32) != 0                  : LUI+ADDI(W)
        int64_t Hi20 = ((Val + 0x800) >> 12) & 0xFFFFF;
        int64_t Lo12 = SignExtend64<12>(Val);

        if (Hi20)
          Res.emplace_back([(${namespace})]::[(${lui})], Hi20);

        if (Lo12 || Hi20 == 0) {
          unsigned AddiOpc = [(${namespace})]::[(${addi})];
          Res.emplace_back(AddiOpc, Lo12);
        }
        return Res;
      }

      int64_t Lo12 = SignExtend64<12>(Val);
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
        if (ShiftAmount > 12 && !isInt<12>(Val)) {
          if (isInt<32>((uint64_t)Val << 12)) {
            // Reduce the shift amount and add zeros to the LSBs so it will match
            // LUI.
            ShiftAmount -= 12;
            Val = (uint64_t)Val << 12;
          }
        }
      }

      generateInstSeqImpl(Val, STI, Res);

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

  InstSeq generateInstSeq(int64_t Val, const MCSubtargetInfo &STI) {
    [(${namespace})]MatInt::InstSeq Res;
    generateInstSeqImpl(Val, STI, Res);
    return Res;
  }
}
#endif