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

package vadl.viam.matching.impl;

import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.types.BuiltInTable;
import vadl.utils.Pair;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has the given {@link BuiltInTable.BuiltIn} and
 * all the inputs from the node also match the given {@link Matcher}.
 * Note that if the node has more inputs then matchers given then the {@code matches}
 * method will return {@code true}.
 */
public class BuiltInMatcher implements Matcher {

  private final Set<BuiltInTable.BuiltIn> builtIns;
  private final Collection<Matcher> matchers;

  public BuiltInMatcher(BuiltInTable.BuiltIn builtIn,
                        Collection<Matcher> matchers) {
    this.builtIns = Set.of(builtIn);
    this.matchers = matchers;
  }

  /**
   * Constructor for matcher.
   *
   * @param builtIns is a list of accepted builtins.
   * @param matchers for children nodes.
   */
  public BuiltInMatcher(Collection<BuiltInTable.BuiltIn> builtIns, List<Matcher> matchers) {
    this.builtIns = new HashSet<>(builtIns);
    this.matchers = matchers;
  }

  public BuiltInMatcher(BuiltInTable.BuiltIn builtIn,
                        Matcher matcher) {
    this.builtIns = Set.of(builtIn);
    this.matchers = List.of(matcher);
  }

  public BuiltInMatcher(BuiltInTable.BuiltIn builtIn,
                        Matcher left, Matcher right) {
    this.builtIns = Set.of(builtIn);
    this.matchers = List.of(left, right);
  }

  @Override
  public boolean matches(Node node) {
    if (node instanceof BuiltInCall && builtIns.contains(((BuiltInCall) node).builtIn())) {
      if (this.matchers.isEmpty()) {
        // Edge case: when no matchers exist and the builtIn is matched then return true.
        return true;
      }

      // The matchers must perfectly fit because the inputs cannot be rearranged.
      var checks = Streams.zip(node.inputs(), this.matchers.stream(),
          Pair::of).toList();

      boolean allOk = true;
      for (var check : checks) {
        var matcher = check.right();
        var inputNode = check.left();

        allOk &= matcher.matches(inputNode);
      }

      return allOk;
    }

    return false;
  }

  @Override
  public Matcher swapOperands() {
    if (this.matchers.size() != 2) {
      throw new RuntimeException("BuiltinMatcher has not the expected number of operands");
    }

    var matchers = this.matchers.stream().toList();
    var first = matchers.get(0);
    var second = matchers.get(1);

    // Swap
    return new BuiltInMatcher(builtIns, List.of(second, first));
  }
}
