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

#ifndef VADL_BUILTINS_H
#define VADL_BUILTINS_H

#include <assert.h>
#include <stdint.h>
#include <stdarg.h>

typedef uint64_t Bits; // Generic 64-bit container of bit patterns
typedef Bits UInt; // Interpreted as an unsigned 64-bit integer
typedef Bits Bool; // Interpreted as an unsigned 64-bit integer
typedef int64_t SInt; // Interpreted as a signed 64-bit integer
typedef uint8_t Width; // Bit width

/*********************
 * HELPER FUNCTIONS
 * *******************/

/*
 * VADL_uextract(val, width):
 *   Interpret 'val' as an unsigned N-bit value,
 *   then return that value zero-extended to 64 bits (just masked).
 */
static inline Bits VADL_uextract(Bits val, Width width) {
    if (width >= 64) {
        return val; /* If width >= 64, keep all 64 bits of val */
    }
    return val & (((uint64_t) 1 << width) - 1ULL);
}

/*
 * VADL_sextract(val, width):
 *   Interpret 'val' as a signed two's complement N-bit value,
 *   sign-extend it to 64 bits.
 */
static inline SInt VADL_sextract(Bits val, Width width) {
    if (width == 0) {
        return 0;
    }
    if (width >= 64) {
        /* If width >= 64, just interpret directly as int64_t. */
        return (SInt) val;
    } {
        /* sign-extend from 'width' bits to 64 bits */
        SInt shift = 64 - width;
        return (SInt) (((SInt) val << shift) >> shift);
    }
}

static inline Bits VADL_mask(Width width) {
    return (width >= 64) ? (Bits) (-1) : (((Bits) 1ULL << width) - 1ULL);
}

/*************************
 * ARITHMETIC OPERATIONS
 * ***********************/

/*-----------------------------------------------------------------------
 *    neg(a : Bits<N>) => -a (two's complement signed)
 *---------------------------------------------------------------------*/
static inline Bits VADL_neg(Bits a, Width aw) {
    SInt x = VADL_sextract(a, aw);
    SInt r = -x;
    return VADL_uextract((Bits) r, aw);
}

/*-----------------------------------------------------------------------
 *    add(a : Bits<N>, b : Bits<N>) => a + b (truncated to N bits)
 *    Non-saturating, "bitwise" add
 *---------------------------------------------------------------------*/
static inline Bits VADL_add(Bits a, Width aw, Bits b, Width bw) {
    Bits x   = VADL_uextract(a, aw);
    Bits y   = VADL_uextract(b, bw);
    Bits sum = x + y;
    return VADL_uextract(sum, aw);
}

/*-----------------------------------------------------------------------
 *    ssatadd(a : SInt<N>, b : SInt<N>) => signed saturating add
 *---------------------------------------------------------------------*/
static inline Bits VADL_ssatadd(Bits a, Width aw, Bits b, Width bw) {
    SInt x       = VADL_sextract(a, aw);
    SInt y       = VADL_sextract(b, bw);
    __int128 sum = (__int128) x + (__int128) y;

    if (aw == 0) {
        return 0ULL;
    }
    /* Range of signed N-bit: [ -2^(aw-1) .. 2^(aw-1) - 1 ] */
    SInt minVal = -(1LL << (aw - 1));
    SInt maxVal = (1LL << (aw - 1)) - 1;

    if (sum > maxVal) sum = maxVal;
    if (sum < minVal) sum = minVal;

    return VADL_uextract(sum, aw);
}

/*-----------------------------------------------------------------------
 *    usatadd(a : UInt<N>, b : UInt<N>) => unsigned saturating add
 *---------------------------------------------------------------------*/
static inline Bits VADL_usatadd(Bits a, Width aw, Bits b, Width bw) {
    Bits x          = VADL_uextract(a, aw);
    Bits y          = VADL_uextract(b, bw);
    __uint128_t sum = (__uint128_t) x + (__uint128_t) y;

    /* Max for aw bits: 2^aw - 1 (unless aw >= 64, in which case it's ~0) */
    Bits maxVal = (aw >= 64)
                      ? (Bits) (-1)
                      : ((1ULL << aw) - 1ULL);

    if (sum > maxVal) {
        sum = maxVal;
    }

    return (Bits) sum;
}

/*-----------------------------------------------------------------------
 *    sub(a : Bits<N>, b : Bits<N>) => a - b (truncated)
 *    Non-saturating, "bitwise" sub
 *---------------------------------------------------------------------*/
static inline Bits VADL_sub(Bits a, Width aw, Bits b, Width bw) {
    Bits x    = VADL_uextract(a, aw);
    Bits y    = VADL_uextract(b, bw);
    Bits diff = x - y; /* In two's complement, sign label doesn't matter */
    return VADL_uextract(diff, aw);
}

/*-----------------------------------------------------------------------
 *    ssatsub(a : SInt<N>, b : SInt<N>) => signed saturating sub
 *---------------------------------------------------------------------*/
static inline Bits VADL_ssatsub(Bits a, Width aw, Bits b, Width bw) {
    SInt x        = VADL_sextract(a, aw);
    SInt y        = VADL_sextract(b, bw);
    __int128 diff = (__int128) x - (__int128) y;

    if (aw == 0) {
        return 0ULL;
    }
    SInt minVal = -(1LL << (aw - 1));
    SInt maxVal = (1LL << (aw - 1)) - 1;

    if (diff > maxVal) diff = maxVal;
    if (diff < minVal) diff = minVal;

    return VADL_uextract((Bits) diff, aw);
}

/*-----------------------------------------------------------------------
 *    usatsub(a : UInt<N>, b : UInt<N>) => unsigned saturating sub
 *---------------------------------------------------------------------*/
static inline Bits VADL_usatsub(Bits a, Width aw, Bits b, Width bw) {
    Bits x        = VADL_uextract(a, aw);
    Bits y        = VADL_uextract(b, bw);
    __int128 diff = (__int128) x - (__int128) y;

    if (diff < 0) {
        diff = 0;
    } else {
        Bits maxVal = (aw >= 64)
                          ? (Bits) (-1)
                          : ((1ULL << aw) - 1ULL);
        if ((__uint128_t) diff > maxVal) {
            diff = maxVal;
        }
    }
    return (Bits) diff;
}

/*-----------------------------------------------------------------------
 *    mul(a : Bits<N>, b : Bits<N>) => truncated to 'outw' bits
 *    Non-saturating "bitwise" multiply
 *---------------------------------------------------------------------*/
static inline Bits VADL_mul(Bits a, Width aw, Bits b, Width bw) {
    Bits x              = VADL_uextract(a, aw);
    Bits y              = VADL_uextract(b, bw);
    __uint128_t product = (__uint128_t) x * (__uint128_t) y;

    __uint128_t mask = (((__uint128_t) 1 << aw) - 1ULL);
    return (Bits) (product & mask);
}

// TODO: Find solution for a/b > 32bit
/*-----------------------------------------------------------------------
 *    smull(a : SInt<N>, b : SInt<N>) => 2*N bits (signed)
 *---------------------------------------------------------------------*/
static inline Bits VADL_smull(Bits a, Width aw, Bits b, Width bw) {
    SInt x           = VADL_sextract(a, aw);
    SInt y           = VADL_sextract(b, bw);
    __int128 product = (__int128) x * (__int128) y;
    Width width      = aw + bw;

    // Check if the product fits within 64 bits (signed range)
    assert(product >= INT64_MIN && product <= INT64_MAX && "Overflow: result exceeds 64 bits");
    return VADL_uextract((Bits) product, width);
}

// TODO: Find solution for a/b > 32bit
/*-----------------------------------------------------------------------
 *     umull(a : UInt<N>, b : UInt<N>) => 2*N bits (unsigned)
 *---------------------------------------------------------------------*/
static inline Bits VADL_umull(Bits a, Width aw, Bits b, Width bw) {
    Bits x              = VADL_uextract(a, aw);
    Bits y              = VADL_uextract(b, bw);
    __uint128_t product = (__uint128_t) x * (__uint128_t) y;
    Width width         = aw + bw;

    // Check if the product fits within 64 bits (unsigned range)
    assert((product >> 64) == 0 && "Overflow: result exceeds 64 bits");
    return VADL_uextract((Bits) product, width);
}

// TODO: Find solution for a/b > 32bit
/*-----------------------------------------------------------------------
 *     sumull(a : SInt<N>, b : UInt<N>) => 2*N bits (signed * unsigned)
 *---------------------------------------------------------------------*/
static inline Bits VADL_sumull(Bits a, Width aw, Bits b, Width bw) {
    SInt x           = VADL_sextract(a, aw);
    Bits y           = VADL_uextract(b, bw);
    __int128 product = (__int128) x * (__int128) y;
    Width width      = aw + bw;
    // Check if the product fits within 64 bits (signed range)
    assert(product >= INT64_MIN && product <= INT64_MAX && "Overflow: result exceeds 64 bits");
    return VADL_uextract((Bits) product, width);
}

/*-----------------------------------------------------------------------
 *     smod(a : SInt<N>, b : SInt<N>) => (a % b), truncated to N bits
 *---------------------------------------------------------------------*/
static inline Bits VADL_smod(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);

    if (y == 0) {
        return 0ULL;
    }
    SInt rem = x % y;
    return VADL_uextract((Bits) rem, aw);
}

/*-----------------------------------------------------------------------
 *     umod(a : UInt<N>, b : UInt<N>) => (a % b), truncated to N bits
 *---------------------------------------------------------------------*/
static inline Bits VADL_umod(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);

    if (y == 0) {
        return 0ULL;
    }
    Bits rem = x % y;
    return VADL_uextract(rem, aw);
}

/*-----------------------------------------------------------------------
 *     sdiv(a : SInt<N>, b : SInt<N>) => (a / b), truncated to N bits
 *---------------------------------------------------------------------*/
static inline Bits VADL_sdiv(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);

    if (y == 0) {
        return 0ULL;
    }

    if (x == INT64_MIN && y == -1) {
        // Return the minimum value, as dividing it by -1 would overflow
        // --> prevents exception on x86 for Bits<64>
        return VADL_uextract((Bits)INT64_MIN, aw);
    }

    SInt quotient = x / y;
    return VADL_uextract((Bits) quotient, aw);
}

/*-----------------------------------------------------------------------
 *     udiv(a : UInt<N>, b : UInt<N>) => (a / b), truncated to N bits
 *---------------------------------------------------------------------*/
static inline Bits VADL_udiv(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);

    if (y == 0) {
        return 0ULL;
    }
    Bits quotient = x / y;
    return VADL_uextract(quotient, aw);
}

/*********************
 * LOGICAL OPERATIONS
 *********************/

/*-----------------------------------------------------------------------
 *    not(a : Bits<N>) => ~a, !a if N == 1
 *---------------------------------------------------------------------*/
static inline Bits VADL_not(Bits a, Width w) {
    Bits x = VADL_uextract(a, w);
    if (w == 1) {
        /* Logical NOT for 1-bit */
        return x ? 0ULL : 1ULL;
    } else {
        /* Bitwise NOT for w > 1 */
        return (~x) & VADL_mask(w);
    }
}

/*-----------------------------------------------------------------------
 *    and(a : Bits<N>, b : Bits<N>) => a & b, a && b if N == 1
 *---------------------------------------------------------------------*/
static inline Bits VADL_and(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    if (aw == 1 && bw == 1) {
        /* Logical AND for 1-bit */
        return (x && y) ? 1ULL : 0ULL;
    } else {
        /* Bitwise AND */
        return (x & y) & VADL_mask(aw);
    }
}

/*-----------------------------------------------------------------------
 *    xor(a : Bits<N>, b : Bits<N>) => a ^ b
 *---------------------------------------------------------------------*/
static inline Bits VADL_xor(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    return (x ^ y) & VADL_mask(aw);
}

/*-----------------------------------------------------------------------
 *    or(a : Bits<N>, b : Bits<N>) => a | b, a || b if N == 1
 *---------------------------------------------------------------------*/
static inline Bits VADL_or(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    if (aw == 1 && bw == 1) {
        /* Logical OR for 1-bit */
        return (x || y) ? 1ULL : 0ULL;
    } else {
        return (x | y) & VADL_mask(aw);
    }
}

/************************
 * COMPARISON OPERATIONS
 ************************/

/*-----------------------------------------------------------------------
 *    equ(a : Bits<N>, b : Bits<N>) => a = b
 *---------------------------------------------------------------------*/
static inline Bits VADL_equ(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    /* Return 1 if equal, else 0 */
    return (x == y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    neq(a : Bits<N>, b : Bits<N>) => a != b
 *---------------------------------------------------------------------*/
static inline Bits VADL_neq(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    return (x != y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    slth(a : SInt<N>, b : SInt<N>) => a < b
 *---------------------------------------------------------------------*/
static inline Bits VADL_slth(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);
    return (x < y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    ulth(a : UInt<N>, b : UInt<N>) => a < b
 *---------------------------------------------------------------------*/
static inline Bits VADL_ulth(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    return (x < y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    sleq(a : SInt<N>, b : SInt<N>) => a <= b
 *---------------------------------------------------------------------*/
static inline Bits VADL_sleq(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);
    return (x <= y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    uleq(a : UInt<N>, b : UInt<N>) => a <= b
 *---------------------------------------------------------------------*/
static inline Bits VADL_uleq(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    return (x <= y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    sgth(a : SInt<N>, b : SInt<N>) => a > b
 *---------------------------------------------------------------------*/
static inline Bits VADL_sgth(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);
    return (x > y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    ugth(a : UInt<N>, b : UInt<N>) => a > b
 *---------------------------------------------------------------------*/
static inline Bits VADL_ugth(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    return (x > y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    sgeq(a : SInt<N>, b : SInt<N>) => a >= b
 *---------------------------------------------------------------------*/
static inline Bits VADL_sgeq(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    SInt y = VADL_sextract(b, bw);
    return (x >= y) ? 1ULL : 0ULL;
}

/*-----------------------------------------------------------------------
 *    ugeq(a : UInt<N>, b : UInt<N>) => a >= b
 *---------------------------------------------------------------------*/
static inline Bits VADL_ugeq(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits y = VADL_uextract(b, bw);
    return (x >= y) ? 1ULL : 0ULL;
}


/************************
 * SHIFTING OPERATIONS
 ************************/

/*-----------------------------------------------------------------------
 *    lsl(a : Bits<N>, b : UInt<M>) => a << b
 *---------------------------------------------------------------------*/
static inline Bits VADL_lsl(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits s = VADL_uextract(b, bw);
    s %= aw;

    Bits shifted = x << s;
    if (aw < 64) {
        shifted &= VADL_mask(aw);
    }
    return shifted;
}

/*-----------------------------------------------------------------------
 *    asr(a : SInt<N>, b : UInt<M>) => a >> b  (arithmetic shift right)
 *---------------------------------------------------------------------*/
static inline Bits VADL_asr(Bits a, Width aw, Bits b, Width bw) {
    SInt x = VADL_sextract(a, aw);
    Bits s = VADL_uextract(b, bw);
    s %= aw;

    SInt shifted = x >> s;
    if (aw < 64) {
        return ((Bits) shifted) & VADL_mask(aw);
    } else {
        return (Bits) shifted;
    }
}

/*-----------------------------------------------------------------------
 *    lsr(a : UInt<N>, b : UInt<M>) => a >> b  (logical shift right)
 *---------------------------------------------------------------------*/
static inline Bits VADL_lsr(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits s = VADL_uextract(b, bw);
    s %= aw;

    return x >> s; /* Already limited to aw bits. */
}

/*-----------------------------------------------------------------------
 *    rol(a : Bits<N>, b : UInt<M>) => a <<> b  (rotate left)
 *---------------------------------------------------------------------*/
static inline Bits VADL_rol(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits s = VADL_uextract(b, bw);

    s %= aw;
    if (s == 0ULL) {
        return x;
    }

    Bits m     = VADL_mask(aw);
    Bits left  = (x << s) & m;
    Bits right = x >> (aw - s);
    return (left | right) & m;
}

/*-----------------------------------------------------------------------
 *    ror(a : Bits<N>, b : UInt<M>) => a <>> b  (rotate right)
 *---------------------------------------------------------------------*/
static inline Bits VADL_ror(Bits a, Width aw, Bits b, Width bw) {
    Bits x = VADL_uextract(a, aw);
    Bits s = VADL_uextract(b, bw);

    s %= aw;
    if (s == 0ULL) {
        return x;
    }

    Bits m     = VADL_mask(aw);
    Bits right = x >> s;
    Bits left  = (x << (aw - s)) & m;
    return (right | left) & m;
}

// TODO: What is the exact semantic of rrx?
/*-----------------------------------------------------------------------
 *    rrx(a : Bits<N>, b : UInt<M>, c : Bool) => Bits<N>
 *    Rotate-right-extend by b bits, each step inserting 'c' from the left.
 *---------------------------------------------------------------------*/
static inline Bits VADL_rrx(Bits a, Width aw, Bits b, Width bw, Bits c, Width cw) {
    Bits x = VADL_uextract(a, aw);
    Bits s = VADL_uextract(b, bw);
    // normalize c to 0 or 1
    c = c != 0;
    s %= aw;

    Bits m   = VADL_mask(aw);
    Bits reg = x & m;

    while (s > 0ULL) {
        /* Shift right by 1, insert 'c' at top (bit aw-1). */
        Bits top = (c & 1ULL) << (aw - 1);
        reg      = ((reg >> 1) | top) & m;
        s--;
    }

    return reg;
}

/**************************
 * BIT COUNTING OPERATIONS
 **************************/

/*-----------------------------------------------------------------------
 *    cob(a : Bits<N>) => number of 1-bits in a
 *---------------------------------------------------------------------*/
static inline Bits VADL_cob(Bits a, Width w) {
    /* Extract the N-bit field, then count ones. */
    Bits x     = VADL_uextract(a, w);
    Bits count = 0ULL;
    while (x != 0ULL) {
        count += (x & 1ULL);
        x >>= 1ULL;
    }
    return count;
}

/*-----------------------------------------------------------------------
 *    czb(a : Bits<N>) => number of 0-bits in a
 *---------------------------------------------------------------------*/
static inline Bits VADL_czb(Bits a, Width w) {
    /* Zero bits = total bits (w) minus number of 1-bits. */
    /* If w=0, result is 0. If w>64, we treat a as 64 bits. */
    if (w == 0) {
        return 0ULL;
    }
    Bits ones = VADL_cob(a, w);
    return (ones > w) ? 0ULL : (w - ones);
}

/*-----------------------------------------------------------------------
 *    clz(a : Bits<N>) => count leading zeros in a
 *---------------------------------------------------------------------*/
static inline Bits VADL_clz(Bits a, Width w) {
    /* We look at only w bits. If w=0, there's nothing to count. */
    if (w == 0) {
        return 0ULL;
    }
    Bits x = VADL_uextract(a, w);

    /* For w <= 64, the leading zeros are how many top bits (from left) are 0.
     * We'll shift the value down until the top bit is set or the value is 0.
     */
    Bits topBitPos = w;

    Bits leading = 0ULL;
    while (leading < w) {
        /* Check the top bit among w bits: bit (w - 1) if any are set */
        Bits topBit = (x >> (w - 1ULL)) & 1ULL;
        if (topBit != 0ULL) {
            break; /* found a 1 => stop */
        }
        leading++;
        x <<= 1ULL; /* shift left so next bit becomes top */
    }
    return leading;
}

/*-----------------------------------------------------------------------
 *    clo(a : Bits<N>) => count leading ones in a
 *---------------------------------------------------------------------*/
static inline Bits VADL_clo(Bits a, Width w) {
    /* If w=0 => 0 leading bits. */
    if (w == 0) {
        return 0ULL;
    }
    Bits x     = VADL_uextract(a, w);

    Bits leading = 0ULL;
    while (leading < w) {
        /* Check top bit among w bits */
        Bits topBit = (x >> (w - 1ULL)) & 1ULL;
        if (topBit == 0ULL) {
            break; /* found a 0 => stop */
        }
        leading++;
        x <<= 1ULL;
    }
    return leading;
}

/*-----------------------------------------------------------------------
 *    cls(a : Bits<N>) => count leading sign bits in a (without sign bit)
 *---------------------------------------------------------------------*/
static inline Bits VADL_cls(Bits a, Width w) {
    /*
     * Interpretation: a is a 2's complement N-bit value.
     * The "leading sign bits" are the bits at top (starting from bit w-2 down)
     * that match the sign bit (bit w-1).
     * The sign bit itself is not counted, so we check from w-2 downwards.
     */
    if (w <= 1) {
        /* If w=0 or w=1, there's no "leading bits" beyond the sign bit. */
        return 0ULL;
    }
    SInt x = VADL_sextract(a, w);

    /* sign bit: 1 if negative, 0 if non-negative */
    SInt signBit = (x < 0) ? 1 : 0;
    Bits leading = 0ULL;

    /* We'll treat x as an N-bit value, ignoring everything else. */
    /* The sign bit is at position (w-1). We'll examine bits [w-2 .. 0]. */
    for (SInt pos = (SInt) w - 2; pos >= 0; pos--) {
        Bits bit = ((Bits) x >> pos) & 1ULL;
        if (bit != (Bits) signBit) {
            break;
        }
        leading++;
    }
    return leading;
}

/*-----------------------------------------------------------------------
 *    ctz(a : Bits<N>) => count trailing zeros in a
 *---------------------------------------------------------------------*/
static inline Bits VADL_ctz(Bits a, Width w) {
    /* We look at only w bits. If w == 0, there's nothing to count. */
    if (w == 0) {
        return 0ULL;
    }
    Bits x = VADL_uextract(a, w);

    Bits trailing = 0ULL;
    while (trailing < w) {
        /* Check least‑significant bit among w bits */
        if ((x & 1ULL) != 0ULL) {
            break; /* found a 1 => stop */
        }
        trailing++;
        x >>= 1ULL; /* shift right so next bit becomes LSB */
    }
    return trailing;
}

/*-----------------------------------------------------------------------
 *    cto(a : Bits<N>) => count trailing ones in a
 *---------------------------------------------------------------------*/
static inline Bits VADL_cto(Bits a, Width w) {
    /* We look at only w bits. If w == 0, there's nothing to count. */
    if (w == 0) {
        return 0ULL;
    }
    Bits x = VADL_uextract(a, w);

    Bits trailing = 0ULL;
    while (trailing < w) {
        /* Check least‑significant bit among w bits */
        if ((x & 1ULL) == 0ULL) {
            break; /* found a 0 => stop */
        }
        trailing++;
        x >>= 1ULL;
    }
    return trailing;
}


/************************
 * MISC
 ************************/

/*-----------------------------------------------------------------------
 *    concat(a: Bits<N>, b: Bits<M>) -> Bits<N + M> (concatenate bits)
 *---------------------------------------------------------------------*/
static inline Bits VADL_concat(Bits a, Width aw, Bits b, Width bw) {
  Width res_w = aw + bw;
  a = VADL_uextract(a, aw);
  b = VADL_uextract(b, bw);
  // shift a << b.width
  Bits shifted = VADL_lsl(a, aw, bw, 32);
  return VADL_or(shifted, res_w, b, res_w);
}

/**
 * Extracts and concatenates specified bit ranges from a 64-bit value.
 *
 * @param v      The 64-bit input value.
 * @param nparts The number of (hi, lo) index pairs provided.
 * @param ...    A variable list of integer pairs: hi1, lo1, hi2, lo2, ..., hin, lon.
 *               Each pair defines a bit range from bit lo to bit hi (inclusive).
 *               Bits are extracted and concatenated in the order provided,
 *               starting from the least significant bit of the result.
 *
 * @return A 64-bit value containing the concatenated bits from the specified ranges.
 *
 * Example:
 *   VADL_slice(0xF0F0, 2, 11, 8, 3, 0)
 *   Extracts bits 11..8 and 3..0 from 0xF0F0 and concatenates them into the result.
 */
static inline Bits VADL_slice(uint64_t v, uint32_t nparts, ...) {
    va_list ap;
    va_start(ap, nparts);

    va_list ap2;
    va_copy(ap2, ap);
    uint32_t total_width = 0;
    for (uint32_t i = 0; i < nparts; ++i) {
        int hi = va_arg(ap2, int);
        int lo = va_arg(ap2, int);
        total_width += (hi - lo + 1);
    }
    va_end(ap2);

    uint64_t result = 0;
    uint32_t out_bit = total_width;

    for (uint32_t i = 0; i < nparts; ++i) {
        int hi = va_arg(ap, int);
        int lo = va_arg(ap, int);
        int width = hi - lo + 1;
        out_bit -= width;
        uint64_t mask = VADL_mask(width);
        uint64_t part = (v >> lo) & mask;
        result |= (part << out_bit);
    }

    va_end(ap);
    return result;
}

#endif //VADL_BUILTINS_H
