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

import java.util.IdentityHashMap;
import java.util.Map;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.PassResults;
import vadl.viam.Format;

/**
 * Utility class for predicates.
 */
public class ImmediatePredicateFunctionProvider {
  /**
   * Get the predicates.
   */
  public static Map<TableGenImmediateRecord, GcbCppFunctionWithBody> generatePredicateFunctions(
      PassResults passResults) {
    return ((CreateFunctionsFromImmediatesPass.Output)
        passResults.lastResultOf(CreateFunctionsFromImmediatesPass.class)).predicates();
  }

  /**
   * Get the predicate functions by field access.
   */
  public static Map<Format.FieldAccess, GcbCppFunctionWithBody> predicateFunctionsByFieldAccess(
      PassResults passResults) {
    var result = new IdentityHashMap<Format.FieldAccess, GcbCppFunctionWithBody>();
    var map = generatePredicateFunctions(passResults);

    for (var entry : map.entrySet()) {
      result.put(entry.getKey().fieldAccessRef(), entry.getValue());
    }

    return result;
  }
}
