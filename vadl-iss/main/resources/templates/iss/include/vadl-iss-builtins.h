// SPDX-FileCopyrightText : © 2024 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

#ifndef VADL_ISS_BUILTINS_H
#define VADL_ISS_BUILTINS_H

#include "vadl-builtins.h"

/*
 * ISS-specific helpers that are not part of the generic builtins but are
 * required by intermediate ISS nodes during code generation.
 *
 * Specifically, implement MULH variants that return the upper half of the
 * 2*N-bit product of two N-bit operands. The exact multiplication semantics
 * depend on the operand signedness.
 */

/*
 * smulh(a : SInt<N>, b : SInt<N>) => Bits<N> (upper N bits of signed* signed)
 */
static inline Bits VADL_smulh(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);
    __int128 product = (__int128) x * (__int128) y;   // 2*N signed product

    // Take the high N bits via arithmetic shift and mask to N bits
    __int128 high = product >> aw;
    return VADL_uextract((Bits) high, aw);
}

/*
 * umulh(a : UInt<N>, b : UInt<N>) => Bits<N> (upper N bits of unsigned* unsigned)
 */
static inline Bits VADL_umulh(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    __uint128_t product = (__uint128_t) x * (__uint128_t) y; // 2*N unsigned product

    __uint128_t high = product >> aw; // logical shift
    return VADL_uextract((Bits) high, aw);
}

/*
 * sumulh(a : SInt<N>, b : UInt<N>) => Bits<N> (upper N bits of signed* unsigned)
 */
static inline Bits VADL_sumulh(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    __int128 product = (__int128) x * (__int128) ((__uint128_t) y); // 2*N signed product

    __int128 high = product >> aw; // arithmetic shift
    return VADL_uextract((Bits) high, aw);
}

#endif // VADL_ISS_BUILTINS_H
