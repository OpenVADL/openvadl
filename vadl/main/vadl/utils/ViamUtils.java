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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.viam.DefProp;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;

/**
 * A set of utility methods that helps when working with the VIAM.
 */
public class ViamUtils {

  /**
   * Finds a Definition by its identifier and type.
   *
   * <p>Example:
   * {@code findDefinitionByIdentAndType(spec,
   * "RV3264IM::RTYPE::imm", Format.Field.class)}
   *
   * @param root            The root Definition to start the search from.
   * @param identifier      The name of the Definition to find.
   * @param definitionClass The type of the Definition to find.
   * @return The first Definition with the given identifier and type, or null if not found.
   */
  @Nullable
  public Definition findDefinitionByIdentAndType(Definition root, String identifier,
                                                 Class<Definition> definitionClass) {
    var result = findDefinitionsByFilter(root,
        (def) -> definitionClass.isInstance(def) && def.identifier.name().equals(identifier));

    root.ensure(result.size() > 1,
        "More than 1 definitions with name %s and type %s found in this root definition",
        identifier, definitionClass.getSimpleName());

    if (result.isEmpty()) {
      return null;
    } else {
      return result.stream().findFirst().get();
    }
  }

  /**
   * Finds all Definitions that match the given filter.
   *
   * @param root   The root Definition to start the search from.
   * @param filter The filter Function to check if a Definition should be included in the result.
   * @return A Set of Definitions that match the given filter.
   */
  public static Set<Definition> findDefinitionsByFilter(Definition root,
                                                        Function<Definition, Boolean> filter) {
    return new DefinitionVisitor.Recursive() {

      private Set<Definition> allDefs = new HashSet<>();

      public Set<Definition> findAllIn(Definition definition) {
        definition.accept(this);
        return this.allDefs;
      }

      @Override
      public void beforeTraversal(Definition definition) {
        super.beforeTraversal(definition);
        if (filter.apply(definition)) {
          allDefs.add(definition);
        }
      }
    }.findAllIn(root);
  }


  public static Stream<DefProp.WithBehavior> findAllWithBehavior(Definition root) {
    return findDefinitionsByFilter(root, d -> d instanceof DefProp.WithBehavior).stream().map(
        DefProp.WithBehavior.class::cast);
  }
}