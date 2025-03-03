// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
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

package vadl.ast;

class Builtins {
  // TODO Replace with BuiltInTable
  static final String[] BUILTIN_FUNCTIONS = new String[] {
      // TODO Clean up actual global built-ins and context-specific built-ins (e.g. "firmware")
      "mnemonic",
      "decimal",
      "hex",
      "register",
      "isInDelaySlot",
      "delayed",
      "start",
      "executable",
      "halt",
      "firmware",
      "laidin",
      "laideq",

      // TODO Legacy functions, only needed for compatibility with old vadl files
      "VADL::mod", // Required by rv3264im, verify if necessary
      "VADL::div", // Required by miniARMv7, verify if necessary
      "VADL::asl", // Required by hexagon, verify if necessary
      "VADL::lth", // Required by mipsiv, verify if necessary
      "VADL::leq", // Required by mipsiv, verify if necessary
      "VADL::gth", // Required by mipsiv, verify if necessary
      "VADL::geq", // Required by mipsiv, verify if necessary

      "VADL::neg",
      "VADL::add",
      "VADL::adds",
      "VADL::addc",
      "VADL::ssatadd",
      "VADL::usatadd",
      "VADL::ssatadds",
      "VADL::usatadds",
      "VADL::ssataddc",
      "VADL::usataddc",
      "VADL::sub",
      "VADL::subsc",
      "VADL::subsb",
      "VADL::subc",
      "VADL::subb",
      "VADL::ssatsub",
      "VADL::usatsub",
      "VADL::ssatsubs",
      "VADL::usatsubs",
      "VADL::ssatsubc",
      "VADL::usatsubc",
      "VADL::ssatsubb",
      "VADL::usatsubb",
      "VADL::mul",
      "VADL::muls",
      "VADL::smull",
      "VADL::umull",
      "VADL::sumull",
      "VADL::smulls",
      "VADL::umulls",
      "VADL::sumulls",
      "VADL::smod",
      "VADL::umod",
      "VADL::smods",
      "VADL::umods",
      "VADL::sdiv",
      "VADL::udiv",
      "VADL::sdivs",
      "VADL::udivs",
      "VADL::not",
      "VADL::and",
      "VADL::ands",
      "VADL::xor",
      "VADL::xors",
      "VADL::or",
      "VADL::ors",
      "VADL::equ",
      "VADL::neq",
      "VADL::slth",
      "VADL::ulth",
      "VADL::sleq",
      "VADL::uleq",
      "VADL::sgth",
      "VADL::ugth",
      "VADL::sgeq",
      "VADL::ugeq",
      "VADL::lsl",
      "VADL::lsls",
      "VADL::lslc",
      "VADL::asr",
      "VADL::lsr",
      "VADL::asrs",
      "VADL::lsrs",
      "VADL::asrc",
      "VADL::lsrc",
      "VADL::rol",
      "VADL::rols",
      "VADL::rolc",
      "VADL::ror",
      "VADL::rors",
      "VADL::rorc",
      "VADL::rrx",
      "VADL::cob",
      "VADL::czb",
      "VADL::clz",
      "VADL::clo",
      "VADL::cls"
  };
}
