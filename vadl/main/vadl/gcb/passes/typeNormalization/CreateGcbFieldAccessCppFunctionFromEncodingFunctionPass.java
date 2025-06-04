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
import vadl.viam.Parameter;
import vadl.viam.Specification;

/**
 * Wraps the encoding function into {@link GcbCppFunctionForFieldAccess}.
 */
public class CreateGcbFieldAccessCppFunctionFromEncodingFunctionPass extends Pass {

  public CreateGcbFieldAccessCppFunctionFromEncodingFunctionPass(
      GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("CreateGcbFieldAccessCppFunctionFromEncodingFunctionPass");
  }

  /**
   * Output of the pass.
   */
  public record Output(Map<Format.FieldEncoding, GcbCppFunctionForFieldAccess> byFunction,
                       Map<Format.Field, GcbCppFunctionForFieldAccess> byField) {

  }

  @Nullable
  @Override
  public Output execute(PassResults passResults,
                        Specification viam) throws IOException {
    var byFunction = new IdentityHashMap<Format.FieldEncoding, GcbCppFunctionForFieldAccess>();
    var byField = new IdentityHashMap<Format.Field, GcbCppFunctionForFieldAccess>();

    // TODO: Reimplement this with the new Format.FieldEncoding
    viam.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
        .flatMap(x -> x.fieldAccesses().stream())
        .map(fieldAccess -> new Pair<>(fieldAccess,
            ensureNonNull(fieldAccess.encoding(),
                () -> Diagnostic.error(
                    "Encoding must not be null. Maybe it does not exist or was not generated?",
                    fieldAccess.location())
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
      Format.FieldEncoding function,
      Format.FieldAccess fieldAccess) {
    return new GcbCppFunctionForFieldAccess(function.identifier,
        // TODO: Fix parameter (currently no parameter)
        new Parameter[] {},
        function.targetField().type(),
        function.behavior(),
        fieldAccess);
  }
}
