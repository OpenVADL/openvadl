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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.passes.relocation.model.RelocationsBeforeElfExpansion;
import vadl.gcb.passes.relocation.model.UserSpecifiedRelocation;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Relocation;

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
      CppFunctionCode relocation,
      RelocationsBeforeElfExpansion original) implements Renderable {

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
  public static List<BaseInfoRecord> getBaseInfoRecords(PassResults passResults, String namespace) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);

    return output.relocationsBeforeElfExpansion().stream().map(
        relocationBeforeExpand -> {
          var generator =
              new ValueRelocationFunctionCodeGenerator(relocationBeforeExpand.relocation(),
                  relocationBeforeExpand.valueRelocation(),
                  new ValueRelocationFunctionCodeGenerator.Options(
                      false, true, Optional.of(namespace + "BaseInfo")
                  ));
          var function = new CppFunctionCode(generator.genFunctionDefinition());
          return new BaseInfoRecord(
              generator.genFunctionName(),
              relocationBeforeExpand.variantKind(),
              function,
              relocationBeforeExpand
          );
        }
    ).filter(distinctByKey(BaseInfoRecord::functionName)).toList();
  }

  /**
   * Get the records.
   */
  public static Map<Relocation, BaseInfoRecord> baseInfoRecordsByUserSpecifiedRelocations(
      PassResults passResults, String namespace) {
    var records = getBaseInfoRecords(passResults, namespace);

    return records.stream()
        .filter(x -> x.original instanceof UserSpecifiedRelocation)
        .collect(
            Collectors.toMap(x -> ((UserSpecifiedRelocation) x.original).relocation(), x -> x));
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
