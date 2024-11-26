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
}

static void generateInstSeqImplForPositiveNumbers(int64_t Val, const MCSubtargetInfo &STI,
                                [(${namespace})]MatInt::InstSeq &Res) {
  auto largestPossibleValue = [(${largestPossibleValueAddi})];
  if(Val <= largestPossibleValue) {
    Res.emplace_back([(${namespace})]::[(${addi})], Val);
  } else {
    auto largestPossibleLi = [(${largestPossibleValue})];
    if(Val >= largestPossibleLi) {
      auto lui = project_range<[(${luiLowBit})], [(${luiHighBit})]> (std::bitset<[(${luiFormatSize})]>(largestPossibleLi)).to_ullong();
      auto addi = project_range<0, [(${addiBitSize})]> (std::bitset<[(${luiFormatSize})]>(largestPossibleLi)).to_ullong();
      Res.emplace_back([(${namespace})]::[(${lui})], lui);
      Res.emplace_back([(${namespace})]::[(${addi})], addi);
      generateInstSeqImplForPositiveNumbers(Val - largestPossibleLi, STI, Res);
    } else {
      auto rest = largestPossibleLi - Val;
      auto lui = project_range<[(${luiLowBit})], [(${luiHighBit})]> (std::bitset<[(${luiFormatSize})]>(rest)).to_ullong();
      auto addi = project_range<0, [(${addiBitSize})]> (std::bitset<[(${luiFormatSize})]>(rest)).to_ullong();
      Res.emplace_back([(${namespace})]::[(${lui})], lui);
      Res.emplace_back([(${namespace})]::[(${addi})], addi);
    }
  }
}


static void generateInstSeqImplForNegativeNumbers(int64_t Val, const MCSubtargetInfo &STI,
                                [(${namespace})]MatInt::InstSeq &Res) {
  auto smallestPossibleValue = [(${smallestPossibleValueAddi})];
  auto abs = std::abs(Val);
  if(abs <= std::abs(smallestPossibleValue)) {
    Res.emplace_back([(${namespace})]::[(${addi})], Val);
  } else {
    auto largestPossibleLi = [(${largestPossibleValue})];
    if(std::abs(Val) >= largestPossibleLi) {
      auto lui = project_range<[(${luiLowBit})], [(${luiHighBit})]> (std::bitset<[(${luiFormatSize})]>(largestPossibleLi)).to_ullong();
      auto addi = project_range<0, [(${addiBitSize})]> (std::bitset<[(${luiFormatSize})]>(largestPossibleLi)).to_ullong();
      Res.emplace_back([(${namespace})]::[(${lui})], lui);
      Res.emplace_back([(${namespace})]::[(${addi})], addi);
      generateInstSeqImplForNegativeNumbers(Val + largestPossibleLi, STI, Res);
    } else {
      auto lui = project_range<[(${luiLowBit})], [(${luiHighBit})]> (std::bitset<[(${luiFormatSize})]>(Val)).to_ullong();
      auto addi = project_range<0, [(${addiBitSize})]> (std::bitset<[(${luiFormatSize})]>(Val)).to_ullong();
      Res.emplace_back([(${namespace})]::[(${lui})], lui);
      Res.emplace_back([(${namespace})]::[(${addi})], addi);
    }
  }
}
}

namespace llvm::[(${namespace})]MatInt {
  InstSeq generateInstSeq(int64_t Val, const MCSubtargetInfo &STI) {
    [(${namespace})]MatInt::InstSeq Res;
    if(Val >= 0) {
      generateInstSeqImplForPositiveNumbers(Val, STI, Res);
    } else {
      generateInstSeqImplForNegativeNumbers(Val, STI, Res);
    }
    return Res;
  }
}
#endif