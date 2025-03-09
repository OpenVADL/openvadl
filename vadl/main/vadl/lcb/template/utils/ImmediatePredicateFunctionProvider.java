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
import java.util.stream.Collectors;
import vadl.cppCodeGen.model.GcbCppFunctionForFieldAccess;
import vadl.gcb.passes.typeNormalization.CreateGcbFieldAccessFunctionFromPredicateFunctionPass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;

/**
 * Utility class for predicates.
 */
public class ImmediatePredicateFunctionProvider {
  /**
   * Get the predicates.
   */
  public static Map<Format.Field, GcbCppFunctionForFieldAccess> generatePredicateFunctions(
      PassResults passResults) {
    return ((CreateGcbFieldAccessFunctionFromPredicateFunctionPass.Output)
        passResults.lastResultOf(CreateGcbFieldAccessFunctionFromPredicateFunctionPass.class))
        .byField()
        .entrySet()
        .stream()
        .map(x -> new Pair<>(x.getKey(), x.getValue()))
        .collect(Collectors.toMap(Pair::left, Pair::right));
  }
}
