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
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;

/**
 * Helper class for baseInfo.
 */
public class BaseInfoFunctionProvider {
  /**
   * A Base Info entry.
   */
  public record BaseInfoRecord(
      String functionName,
      VariantKind variantKind,
      CppFunctionCode relocation) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "functionName", functionName,
          "variantKind", variantKind,
          "relocation", relocation
      );
    }
  }

  /**
   * Get the records.
   */
  public static List<BaseInfoRecord> getBaseInfoRecords(PassResults passResults) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var elfRelocations = output.elfRelocations();
    return elfRelocations.stream()
        .map(relocation -> {
          var generator =
              new ValueRelocationFunctionCodeGenerator(relocation, relocation.valueRelocation(),
                  new ValueRelocationFunctionCodeGenerator.Options(
                      false, true
                  ));
          var function = new CppFunctionCode(generator.genFunctionDefinition());
          return new BaseInfoRecord(
              generator.genFunctionName(),
              relocation.variantKind(),
              function
          );
        })
        .toList();
  }
}
