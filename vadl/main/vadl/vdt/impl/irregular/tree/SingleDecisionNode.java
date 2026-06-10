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

package vadl.vdt.impl.irregular.tree;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Select the child based on a single matching bit pattern.
 */
public class SingleDecisionNode implements InnerNode {

  private final BitPattern pattern;
  private final Node matchingChild;

  @Nullable
  private final Node otherChild;

  /**
   * Whether to check for matching (or unmatching) of the pattern.
   */
  private final boolean match;

  /**
   * Creates a new inner node.
   *
   * @param pattern       the pattern to check
   * @param matchingChild the child to select upon matching the pattern
   * @param otherChild    the child to select if the pattern does not match
   */
  public SingleDecisionNode(BitPattern pattern, Node matchingChild, @Nullable Node otherChild) {
    this.pattern = pattern;
    this.match = true; // default to matching
    this.matchingChild = matchingChild;
    this.otherChild = otherChild;
  }

  /**
   * Creates a new inner node.
   *
   * @param pattern       the pattern to check
   * @param match         whether to check for matching (or unmatching) of the pattern
   * @param matchingChild the child to select upon matching the pattern
   * @param otherChild    the child to select if the pattern does not match
   */
  public SingleDecisionNode(BitPattern pattern, boolean match, Node matchingChild,
                            @Nullable Node otherChild) {
    this.pattern = pattern;
    this.match = match;
    this.matchingChild = matchingChild;
    this.otherChild = otherChild;
  }

  @Override
  public Node decide(BitVector insn) {

    // extend/truncate the instruction to the relevant bits before testing
    final BitVector i = insn
        .rightPad(pattern.width(), false);

    final boolean matches = pattern.test(i);

    if (match) {

      if (matches) {
        return matchingChild;
      }

      if (otherChild != null) {
        return otherChild;
      }

      throw new RuntimeException("No decision found for " + insn);
    }

    if (!matches) {
      return matchingChild;
    }

    if (otherChild != null) {
      return otherChild;
    }

    throw new RuntimeException("No decision found for " + insn);
  }

  public boolean isMatch() {
    return match;
  }

  public BitPattern getPattern() {
    return pattern;
  }

  public Node getMatchingChild() {
    return matchingChild;
  }

  @Nullable
  public Node getOtherChild() {
    return otherChild;
  }

  @Override
  public Collection<Node> children() {
    if (otherChild == null) {
      return Set.of(matchingChild);
    }
    return Set.of(matchingChild, otherChild);
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
