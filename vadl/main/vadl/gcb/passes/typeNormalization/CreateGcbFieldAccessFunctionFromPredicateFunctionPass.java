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

package vadl.gcb.passes.typeNormalization;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.GcbCppFunctionForFieldAccess;
import vadl.error.Diagnostic;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * Wraps the predicate function into {@link GcbCppFunctionForFieldAccess}.
 */
public class CreateGcbFieldAccessFunctionFromPredicateFunctionPass extends Pass {
  public CreateGcbFieldAccessFunctionFromPredicateFunctionPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("CreateGcbFieldAccessFunctionFromPredicateFunctionPass");
  }

  /**
   * Output of the pass.
   */
  public record Output(Map<Function, GcbCppFunctionForFieldAccess> byFunction,
                Map<Format.Field, GcbCppFunctionForFieldAccess> byField) {

  }

  @Nullable
  @Override
  public Output execute(PassResults passResults,
                        Specification viam) throws IOException {
    var byFunction = new IdentityHashMap<Function, GcbCppFunctionForFieldAccess>();
    var byField = new IdentityHashMap<Format.Field, GcbCppFunctionForFieldAccess>();

    viam.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(x -> x.fieldAccesses().stream())
        .map(fieldAccess -> new Pair<>(fieldAccess,
            ensureNonNull(fieldAccess.predicate(),
                () -> Diagnostic.error(
                    "Predicate must not be null. Maybe it does not exist or was not generated?",
                    fieldAccess.sourceLocation())
            )))
        .forEach(pair -> {
          var function = createGcbFieldAccessCppFunction(pair.right(), pair.left());
          byFunction.put(pair.right(), function);
          byField.put(pair.left().fieldRef(), function);
        });

    return new Output(byFunction, byField);
  }

  /**
   * Wraps the {@code function} into a {@link GcbCppFunctionForFieldAccess}.
   */
  private GcbCppFunctionForFieldAccess createGcbFieldAccessCppFunction(
      Function function,
      Format.FieldAccess fieldAccess) {
    return new GcbCppFunctionForFieldAccess(function.identifier,
        function.parameters(),
        function.returnType(),
        function.behavior(),
        fieldAccess);
  }

}
