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

import vadl.viam.graph.dependency.BuiltInCall;

/**
 * A dispatcher that handles all {@code VADL::*} built-ins.
 * This dispatcher comes with an empty default implementation for each built-in.
 */
public interface VadlBuiltInEmptyNoStatusDispatcher<T> extends VadlBuiltInNoStatusDispatcher<T> {

  @Override
  default void handleNEG(T input) {
  }

  @Override
  default void handleADD(T input) {
  }

  @Override
  default void handleSSATADD(T input) {
  }

  @Override
  default void handleUSATADD(T input) {
  }

  @Override
  default void handleSUB(T input) {
  }

  @Override
  default void handleSSATSUB(T input) {
  }

  @Override
  default void handleUSATSUB(T input) {
  }

  @Override
  default void handleMUL(T input) {
  }

  @Override
  default void handleUMULL(T input) {
  }

  @Override
  default void handleSMULL(T input) {
  }

  @Override
  default void handleSUMULL(T input) {
  }

  @Override
  default void handleSMOD(T input) {
  }

  @Override
  default void handleUMOD(T input) {
  }

  @Override
  default void handleSDIV(T input) {
  }

  @Override
  default void handleUDIV(T input) {
  }

  @Override
  default void handleNOT(T input) {
  }

  @Override
  default void handleAND(T input) {
  }

  @Override
  default void handleXOR(T input) {
  }

  @Override
  default void handleOR(T input) {
  }

  @Override
  default void handleEQU(T input) {
  }

  @Override
  default void handleNEQ(T input) {
  }

  @Override
  default void handleSLTH(T input) {
  }

  @Override
  default void handleULTH(T input) {
  }

  @Override
  default void handleSLEQ(T input) {
  }

  @Override
  default void handleULEQ(T input) {
  }

  @Override
  default void handleSGTH(T input) {
  }

  @Override
  default void handleUGTH(T input) {
  }

  @Override
  default void handleSGEQ(T input) {
  }

  @Override
  default void handleUGEQ(T input) {
  }

  @Override
  default void handleLSL(T input) {
  }

  @Override
  default void handleASR(T input) {
  }

  @Override
  default void handleLSR(T input) {
  }

  @Override
  default void handleROL(T input) {
  }

  @Override
  default void handleROR(T input) {
  }

  @Override
  default void handleRRX(T input) {
  }

  @Override
  default void handleCOB(T input) {
  }

  @Override
  default void handleCZB(T input) {
  }

  @Override
  default void handleCLZ(T input) {
  }

  @Override
  default void handleCLO(T input) {
  }

  @Override
  default void handleCLS(T input) {
  }

  @Override
  default void handleCTZ(T input) {
  }

  @Override
  default void handleCTO(T input) {
  }

  @Override
  default void handleFSQRT(T input) {
  }

  @Override
  default void handleFADD(T input) {
  }

  @Override
  default void handleFSUB(T input) {
  }

  @Override
  default void handleFMUL(T input) {
  }

  @Override
  default void handleFDIV(T input) {
  }

  @Override
  default void handleFMADD(T input) {
  }

  @Override
  default void handleFMSUB(T input) {
  }

  @Override
  default void handleFNMADD(T input) {
  }

  @Override
  default void handleFNMSUB(T input) {
  }

  @Override
  default void handleFMIN(T input) {
  }

  @Override
  default void handleFMAX(T input) {
  }

  @Override
  default void handleFLT(T input) {
  }

  @Override
  default void handleFLE(T input) {
  }

  @Override
  default void handleFEQ(T input) {
  }

  @Override
  default void handleFCVTFF(T input) {
  }

  @Override
  default void handleFCVTFF2(T input) {
  }

  @Override
  default void handleFCVTFSS(T input) {
  }

  @Override
  default void handleFCVTFSD(T input) {
  }

  @Override
  default void handleFCVTFUS(T input) {
  }

  @Override
  default void handleFCVTFUD(T input) {
  }

  @Override
  default void handleFCVTSSF(T input) {
  }

  @Override
  default void handleFCVTSDF(T input) {
  }

  @Override
  default void handleFCVTUSF(T input) {
  }

  @Override
  default void handleFCVTUDF(T input) {
  }

  @Override
  default void handleFCVTSSF2(T input) {
  }

  @Override
  default void handleFCVTSDF2(T input) {
  }

  @Override
  default void handleFCVTUSF2(T input) {
  }

  @Override
  default void handleFCVTUDF2(T input) {
  }

  @Override
  default void handleFISINF(T input) {
  }

  @Override
  default void handleFISZERO(T input) {
  }

  @Override
  default void handleFISNEG(T input) {
  }

  @Override
  default void handleFISDENORM(T input) {
  }

  @Override
  default void handleFISSNAN(T input) {
  }

  @Override
  default void handleFISQNAN(T input) {
  }

  @Override
  default void handleConcat(T input) {
  }
}
