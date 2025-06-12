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

package vadl.lcb.template.utils;

import java.util.List;
import java.util.Map;
import vadl.cppCodeGen.model.GcbCppEncodeFunction;
import vadl.cppCodeGen.model.GcbCppEncodingWrapperFunction;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;

/**
 * Utility class for encodings.
 */
public class ImmediateEncodingFunctionProvider {
  /**
   * Get the encoding functions.
   */
  public static Map<Instruction, List<GcbCppEncodeFunction>> generateEncodeFunctions(
      PassResults passResults) {
    return ((CreateFunctionsFromImmediatesPass.Output)
        passResults.lastResultOf(CreateFunctionsFromImmediatesPass.class)).encodings();
  }

  /**
   * Get the encoding functions.
   */
  public static Map<Instruction, GcbCppEncodingWrapperFunction> generateEncodeWrapperFunctions(
      PassResults passResults) {
    return ((CreateFunctionsFromImmediatesPass.Output)
        passResults.lastResultOf(CreateFunctionsFromImmediatesPass.class)).encodingsWrappers();
  }
}
