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

package vadl.iss;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Utils for classifying and handling side effects.
 */
public class SideEffectUtils {

  /**
   * The side effect types.
   *
   * <p>There are currently the following types:
   * <ol>
   *   <li>{@code MEM}: Memory writes (and any instruction that may cause a TCG TB to be recompiled
   *   and re-executed by accessing MMIO memory)</li>
   *   <li>{@code SE}: All other side effects (e.g. register writes).</li>
   *   <li>{@code PC}: All side effects causing an instruction exit (writes to the program counter
   *   and exception raises)</li>
   * </ol>
   *
   * <p>Each type also has an ordinal, which defines the (non-strict) total order in which
   * side effects must be in. If two side effects have the same type, they may be ordered
   * in any way. If they have different types, then they must be ordered according to their
   * type ordinals: {@code MEM < SE < PC}.
   */
  public enum SideEffectType {
    MEM(0), SE(1), PC(2);

    private final int ord;

    SideEffectType(int ord) {
      this.ord = ord;
    }

    /**
     * The ordinal of the type.
     */
    public int ord() {
      return ord;
    }

    /**
     * Classifies the given side effect into one of three types
     * (see {@link SideEffectType}).
     *
     * @param node The side effect node to classify.
     * @return The class (or type) of the side effect.
     */
    public static SideEffectType classify(SideEffectNode node) {
      if (node instanceof WriteMemNode) {
        return SideEffectType.MEM;
      } else if ((node instanceof WriteRegTensorNode write && write.isPcAccess())
          || (node instanceof ProcCallNode procCall && procCall.exceptionRaise())) {
        return SideEffectType.PC;
      }
      return SideEffectType.SE;
    }
  }

  /**
   * Stores a set of side effect types (see {@link SideEffectType}).
   */
  public record SideEffectTypes(Set<SideEffectType> types) {
    public static SideEffectTypes empty() {
      return new SideEffectTypes(Set.of());
    }

    public static SideEffectTypes of(SideEffectNode node) {
      return of(Set.of(SideEffectType.classify(node)));
    }

    public static SideEffectTypes of(Set<SideEffectType> types) {
      return new SideEffectTypes(types);
    }

    /**
     * Returns a set union of this and the given types.
     */
    public SideEffectTypes combine(SideEffectTypes other) {
      return new SideEffectTypes(
          Stream.concat(other.types.stream(), types.stream()).collect(Collectors.toSet()));
    }

    /**
     * Returns whether the given type is in the type set.
     */
    public boolean contains(SideEffectType type) {
      return types.contains(type);
    }

    /**
     * Returns whether the given type is covered by the type set.
     *
     * <p>A type is covered if its ordinal is inside the range defined by the minimum
     * and maximum type ordinal in the set (inclusive).
     */
    public boolean covers(SideEffectType type) {
      return !types.isEmpty() && min() <= type.ord() && type.ord() <= max();
    }

    /**
     * Returns the smallest type ordinal in the set.
     *
     * @throws IllegalStateException If the set is empty.
     */
    private int min() {
      return types.stream().mapToInt(SideEffectType::ord).min()
          .orElseThrow(() -> new IllegalStateException("Statement without side effects"));
    }

    /**
     * Returns the largest type ordinal in the set.
     *
     * @throws IllegalStateException If the set is empty.
     */
    private int max() {
      return types.stream().mapToInt(SideEffectType::ord).max()
          .orElseThrow(() -> new IllegalStateException("Statement without side effects"));
    }

    /**
     * Returns whether the set covers MEM and SE but not PC.
     */
    public boolean isMemSe() {
      return min() == SideEffectType.MEM.ord() && max() == SideEffectType.SE.ord();
    }

    /**
     * Returns whether the set covers SE and PC but not MEM.
     */
    public boolean isSePc() {
      return min() == SideEffectType.SE.ord() && max() == SideEffectType.PC.ord();
    }

    /**
     * Returns whether the set covers MEM, SE and PC.
     */
    public boolean isMemPc() {
      return min() == SideEffectType.MEM.ord() && max() == SideEffectType.PC.ord();
    }

    /**
     * Returns whether this and the given set conflict.
     *
     * <p>Two type set conflict, if there is no way to order them such that the resulting
     * list of types is ordered.
     *
     * <p>E.g. the two sets {@code {MEM, SE}} and {@code {MEM, SE}} cannot be ordered, but
     * {@code {MEM, SE}} and {@code {SE, PC}} can.
     */
    public boolean conflictsWith(SideEffectTypes other) {
      if (types.isEmpty() || other.types.isEmpty()) {
        return false;
      }
      return max() > other.min() && min() < other.max();
    }

    /**
     * Defines a (non-strict) total ordering of type sets. Sorting type sets using this
     * comparison function results in a valid ordering of all their types, provided that
     * none of the sets are conflicting (see {@link #conflictsWith(SideEffectTypes)}).
     *
     * @throws IllegalStateException If this and the given set are conflicting.
     */
    public int compare(SideEffectTypes other) {
      if (conflictsWith(other)) {
        throw new IllegalStateException("Comparing two conflicting side effect type lists");
      }
      if (types.equals(other.types)) {
        return 0;
      } else if (types.isEmpty()) {
        return -1;
      } else if (other.types.isEmpty()) {
        return 1;
      }
      int comp = min() - other.min();
      return comp != 0 ? comp : max() - other.max();
    }
  }
}
