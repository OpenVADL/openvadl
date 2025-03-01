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

package vadl.utils;

import vadl.types.BuiltInTable;

/**
 * A dispatcher that handles all {@code VADL::*} built-ins.
 */
public interface VadlBuiltInNoStatusDispatcher<T> {

  /**
   * Dispatches the built-in with the given input.
   *
   * @return false if no handler was found, true otherwise
   */
  default boolean dispatch(T input, BuiltInTable.BuiltIn builtIn) {
    if (builtIn == BuiltInTable.NEG) {
      handleNEG(input);
    } else if (builtIn == BuiltInTable.ADD) {
      handleADD(input);
    } else if (builtIn == BuiltInTable.SSATADD) {
      handleSSATADD(input);
    } else if (builtIn == BuiltInTable.USATADD) {
      handleUSATADD(input);
    } else if (builtIn == BuiltInTable.SUB) {
      handleSUB(input);
    } else if (builtIn == BuiltInTable.SSATSUB) {
      handleSSATSUB(input);
    } else if (builtIn == BuiltInTable.USATSUB) {
      handleUSATSUB(input);
    } else if (builtIn == BuiltInTable.MUL) {
      handleMUL(input);
    } else if (builtIn == BuiltInTable.UMULL) {
      handleUMULL(input);
    } else if (builtIn == BuiltInTable.SMULL) {
      handleSMULL(input);
    } else if (builtIn == BuiltInTable.SUMULL) {
      handleSUMULL(input);
    } else if (builtIn == BuiltInTable.SMOD) {
      handleSMOD(input);
    } else if (builtIn == BuiltInTable.UMOD) {
      handleUMOD(input);
    } else if (builtIn == BuiltInTable.SDIV) {
      handleSDIV(input);
    } else if (builtIn == BuiltInTable.UDIV) {
      handleUDIV(input);
    } else if (builtIn == BuiltInTable.NOT) {
      handleNOT(input);
    } else if (builtIn == BuiltInTable.AND) {
      handleAND(input);
    } else if (builtIn == BuiltInTable.XOR) {
      handleXOR(input);
    } else if (builtIn == BuiltInTable.OR) {
      handleOR(input);
    } else if (builtIn == BuiltInTable.EQU) {
      handleEQU(input);
    } else if (builtIn == BuiltInTable.NEQ) {
      handleNEQ(input);
    } else if (builtIn == BuiltInTable.SLTH) {
      handleSLTH(input);
    } else if (builtIn == BuiltInTable.ULTH) {
      handleULTH(input);
    } else if (builtIn == BuiltInTable.SLEQ) {
      handleSLEQ(input);
    } else if (builtIn == BuiltInTable.ULEQ) {
      handleULEQ(input);
    } else if (builtIn == BuiltInTable.SGTH) {
      handleSGTH(input);
    } else if (builtIn == BuiltInTable.UGTH) {
      handleUGTH(input);
    } else if (builtIn == BuiltInTable.SGEQ) {
      handleSGEQ(input);
    } else if (builtIn == BuiltInTable.UGEQ) {
      handleUGEQ(input);
    } else if (builtIn == BuiltInTable.LSL) {
      handleLSL(input);
    } else if (builtIn == BuiltInTable.ASR) {
      handleASR(input);
    } else if (builtIn == BuiltInTable.LSR) {
      handleLSR(input);
    } else if (builtIn == BuiltInTable.ROL) {
      handleROL(input);
    } else if (builtIn == BuiltInTable.ROR) {
      handleROR(input);
    } else if (builtIn == BuiltInTable.RRX) {
      handleRRX(input);
    } else if (builtIn == BuiltInTable.COB) {
      handleCOB(input);
    } else if (builtIn == BuiltInTable.CZB) {
      handleCZB(input);
    } else if (builtIn == BuiltInTable.CLZ) {
      handleCLZ(input);
    } else if (builtIn == BuiltInTable.CLO) {
      handleCLO(input);
    } else if (builtIn == BuiltInTable.CLS) {
      handleCLS(input);
    } else {
      return false;
    }
    return true;
  }

  void handleNEG(T input);

  void handleADD(T input);

  void handleSSATADD(T input);

  void handleUSATADD(T input);

  void handleSUB(T input);

  void handleSSATSUB(T input);

  void handleUSATSUB(T input);

  void handleMUL(T input);

  void handleUMULL(T input);

  void handleSMULL(T input);

  void handleSUMULL(T input);

  void handleSMOD(T input);

  void handleUMOD(T input);

  void handleSDIV(T input);

  void handleUDIV(T input);

  void handleNOT(T input);

  void handleAND(T input);

  void handleXOR(T input);

  void handleOR(T input);

  void handleEQU(T input);

  void handleNEQ(T input);

  void handleSLTH(T input);

  void handleULTH(T input);

  void handleSLEQ(T input);

  void handleULEQ(T input);

  void handleSGTH(T input);

  void handleUGTH(T input);

  void handleSGEQ(T input);

  void handleUGEQ(T input);

  void handleLSL(T input);

  void handleASR(T input);

  void handleLSR(T input);


  void handleROL(T input);

  void handleROR(T input);

  void handleRRX(T input);

  void handleCOB(T input);

  void handleCZB(T input);

  void handleCLZ(T input);

  void handleCLO(T input);

  void handleCLS(T input);

}
