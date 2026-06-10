// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

#ifndef VADL_BUILTINS_HPP
#define VADL_BUILTINS_HPP

#include <string>

/*-----------------------------------------------------------------------
 *    concat_str(a: String<N>, b: String<M>) -> String<X>
 *---------------------------------------------------------------------*/
static inline std::string VADL_concat_str(std::string a, std::string b) {
    return a + b;
}

/*-----------------------------------------------------------------------
 *    fits_in_bit_width(value: int64_t, width: unsigned) -> bool
 *---------------------------------------------------------------------*/
static inline bool VADL_fits_in_bit_width(int64_t value, unsigned width) {
    if (width >= 64) return true;

    // build mask for where bits above width are 1, e.g.:
    // 00000001
    // 00010000 after shift
    // 00001111 after -1
    // 11110000 after ~
    int64_t mask = ~((INT64_C(1) << width) - 1);

    // only bits above width remain in upper
    int64_t upper = value & mask;

    // then the value fits if all remaining bits are either:
    // 0 (value is positive)
    // 1 (equal to the mask)
    return upper == 0 || upper == mask;
}

#endif //VADL_BUILTINS_HPP