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

package vadl.cppCodeGen.passes.typeNormalization;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.pass.Pass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts bit mask to ensure that the code generation works.
 * This pass does mutate the {@link Specification} because it needs to update the {@link Function}
 * which is immutable.
 */
public abstract class CppTypeNormalizationPass extends Pass {
  public CppTypeNormalizationPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  /**
   * A container for storing the result of {@link CppTypeNormalizationPass}.
   */
  public static class NormalisedTypeResult {
    private final IdentityHashMap<Function, GcbFieldAccessCppFunction> functions =
        new IdentityHashMap<>();
    private final IdentityHashMap<Format.Field, GcbFieldAccessCppFunction> fields =
        new IdentityHashMap<>();

    private void add(Function key, Format.FieldAccess key2, GcbFieldAccessCppFunction value) {
      functions.put(key, value);
      fields.put(key2.fieldRef(), value);
    }

    @Nullable
    public GcbFieldAccessCppFunction byFunction(Function key) {
      return functions.get(key);
    }

    public Collection<Map.Entry<Function, GcbFieldAccessCppFunction>> functions() {
      return functions.entrySet();
    }

    public Collection<Map.Entry<Format.Field, GcbFieldAccessCppFunction>> fields() {
      return fields.entrySet();
    }
  }

  /**
   * Get a list of functions on which the pass should be applied on.
   */
  protected abstract Stream<Pair<Format.FieldAccess, Function>> getApplicable(Specification viam);

  /**
   * Converts a given {@code function} into a {@link GcbFieldAccessCppFunction} which has
   * cpp conforming types.
   */
  protected abstract GcbFieldAccessCppFunction liftFunction(Format.FieldAccess fieldAccess);

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var results = new NormalisedTypeResult();

    getApplicable(viam).forEach(pair -> {
      var field = pair.left();
      var function = pair.right();
      var cppFunction = liftFunction(field);
      results.add(function, field, cppFunction);
    });

    return results;
  }

  /**
   * Wraps the {@code function} into a {@link GcbFieldAccessCppFunction}.
   */
  public static GcbFieldAccessCppFunction createGcbFieldAccessCppFunction(
      Function function,
      Format.FieldAccess fieldAccess) {
    return new GcbFieldAccessCppFunction(function.identifier,
        function.parameters(),
        function.returnType(),
        function.behavior(),
        fieldAccess);
  }

  /**
   * Wraps the {@code function} into a {@link GcbImmediateExtractionCppFunction}.
   */
  public static GcbImmediateExtractionCppFunction createGcbRelocationCppFunction(
      Function function) {
    return new GcbImmediateExtractionCppFunction(function.identifier,
        function.parameters(),
        function.returnType(),
        function.behavior());
  }
}
