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

import java.util.Map;
import vadl.cppCodeGen.model.GcbCppAccessFunction;
import vadl.cppCodeGen.model.GcbCppFunctionBodyLess;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.PassResults;

/**
 * Utility class for decodings.
 */
public class ImmediateDecodingFunctionProvider {
  /**
   * Get the decoding functions.
   */
  public static Map<TableGenImmediateRecord, GcbCppAccessFunction> generateDecodeFunctions(
      PassResults passResults) {
    return ((CreateFunctionsFromImmediatesPass.Output)
        passResults.lastResultOf(CreateFunctionsFromImmediatesPass.class)).decodings();
  }

  /**
   * Get the decoding wrapper functions.
   */
  public static Map<TableGenImmediateRecord, GcbCppFunctionBodyLess> generateDecodeWrapperFunctions(
      PassResults passResults) {
    return ((CreateFunctionsFromImmediatesPass.Output)
        passResults.lastResultOf(CreateFunctionsFromImmediatesPass.class)).decodingWrappers();
  }
}
