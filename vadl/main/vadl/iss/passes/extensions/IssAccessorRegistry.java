// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.extensions;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.viam.graph.Node;

/**
 * Central registry of emitted ISS accessor descriptors.
 *
 * <p>This is the result of {@code IssRegisterAccessInfoRetrievalPass}. It contains both raw
 * base-register accessor descriptors and alias-surface accessor descriptors, and provides lookup
 * by canonical keys for later code generation.
 */
public final class IssAccessorRegistry {

  private final Set<RegInfo.BaseAccessorDescriptor> baseAccessors = new HashSet<>();
  private final Map<RegInfo.BaseAccessorDescriptor.Key, RegInfo.BaseAccessorDescriptor> baseByKey =
      new HashMap<>();
  private final Set<RegInfo.AliasAccessorDescriptor> aliasAccessors = new HashSet<>();

  /**
   * Records a base accessor descriptor under the canonical key derived from the originating node.
   */
  public void recordBaseAccessor(Node origin, RegInfo.BaseAccessorDescriptor descriptor) {
    baseAccessors.add(descriptor);
    baseByKey.put(RegInfo.BaseAccessorDescriptor.Key.ofOrigin(origin), descriptor);
  }

  /**
   * Adds a base accessor descriptor that is not tied to a specific lowered node instance.
   */
  public void addBaseAccessor(RegInfo.BaseAccessorDescriptor descriptor) {
    baseAccessors.add(descriptor);
    baseByKey.put(descriptor.key(), descriptor);
  }

  /**
   * Resolves the collected base accessor descriptor for the given lowered access node.
   */
  public RegInfo.BaseAccessorDescriptor baseAccessorDescriptor(Node origin) {
    var descriptor = baseByKey.get(RegInfo.BaseAccessorDescriptor.Key.ofOrigin(origin));
    origin.ensure(descriptor != null,
        "Missing base accessor descriptor for %s. "
            + "The ISS register-access retrieval pass should have collected it before codegen.",
        origin.getClass().getSimpleName());
    return descriptor;
  }

  /**
   * Adds one alias-surface accessor descriptor to the registry.
   */
  public void addAliasAccessor(RegInfo.AliasAccessorDescriptor descriptor) {
    aliasAccessors.add(descriptor);
  }

  /**
   * Returns all collected base accessor descriptors in stable emission order.
   */
  public List<RegInfo.BaseAccessorDescriptor> baseAccessors() {
    return baseAccessors.stream()
        .sorted(Comparator.comparing(RegInfo.BaseAccessorDescriptor::accessType)
            .thenComparing(RegInfo.BaseAccessorDescriptor::name))
        .toList();
  }

  /**
   * Returns alias accessor descriptors filtered by access type and backend in stable emission
   * order.
   */
  public List<RegInfo.AliasAccessorDescriptor> aliasAccessors(RegInfo.AccessType type,
                                                              RegInfo.BackendKind backendKind) {
    return aliasAccessors.stream()
        .filter(a -> a.accessType() == type && a.backendKind() == backendKind)
        .sorted(Comparator.comparing(RegInfo.AliasAccessorDescriptor::accessorBaseName))
        .toList();
  }

  public Collection<RegInfo.AliasAccessorDescriptor> allAliasAccessors() {
    return List.copyOf(aliasAccessors);
  }
}
