// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.vdt.target.common;

import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;
import static vadl.vdt.utils.BitVectorUtils.fittingPowerOfTwo;
import static vadl.vdt.utils.PatternUtils.combinePatterns;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.impl.regular.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.viam.Instruction;

/**
 * Compute the bit patterns checked by the VDT for each instruction.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = Map.class)
public class CheckedBitsCollector implements Visitor<Map<Instruction, BitPattern>> {

  private final Node tree;
  private final int insnWidth;

  /**
   * Construct the checked-bits collector.
   *
   * @param tree The vadl decode tree.
   */
  public CheckedBitsCollector(Node tree) {
    this.tree = tree;
    this.insnWidth = fittingPowerOfTwo(statistics(tree).getMaxInstructionWidth());
  }

  /**
   * Generate the checked-bits by leaf node (i.e. instructions).
   *
   * @return the instructions.
   */
  public Map<Instruction, BitPattern> collect() {
    var result = tree.accept(this);
    if (result == null) {
      return Map.of();
    }
    return result;
  }

  @Nullable
  @Override
  public Map<Instruction, BitPattern> visit(LeafNode node) {
    return Map.of(node.instruction().source(), BitPattern.empty(insnWidth));
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public Map<Instruction, BitPattern> visit(InnerNode node) {
    return (Map<Instruction, BitPattern>) CheckedBitsCollectorDispatcher.dispatch(this, node);
  }

  /**
   * Handler for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the checked bits
   */
  @Handler
  public Map<Instruction, BitPattern> handle(InnerNodeImpl node) {

    var result = new LinkedHashMap<Instruction, BitPattern>();

    if (node.getFallback() != null) {
      // For the fallback node we cannot mark any bits as 'checked'.
      result.putAll(node.getFallback().accept(this));
    }

    for (var entry : node.getChildren().entrySet()) {

      final Map<Instruction, BitPattern> children = entry.getValue().accept(this);
      if (children == null) {
        throw new IllegalStateException("Expected child entries to exist");
      }

      for (var child : children.entrySet()) {
        var pattern = entry.getKey();
        var childPattern = child.getValue();

        result.put(child.getKey(), combinePatterns(pattern, childPattern));
      }
    }

    return result;
  }

  /**
   * Handler for {@link MultiDecisionNode}.
   *
   * @param node the inner node
   * @return the checked bits
   */
  @Handler
  public Map<Instruction, BitPattern> handle(MultiDecisionNode node) {

    var result = new LinkedHashMap<Instruction, BitPattern>();

    for (var entry : node.getChildren().entrySet()) {

      final Map<Instruction, BitPattern> children = entry.getValue().accept(this);
      if (children == null) {
        throw new IllegalStateException("Expected child entries to exist");
      }

      for (var child : children.entrySet()) {

        var pattern = entry.getKey();
        var childPattern = child.getValue();

        result.put(child.getKey(), combinePatterns(pattern, childPattern));
      }
    }

    return result;
  }

  /**
   * Handler for {@link SingleDecisionNode}.
   *
   * @param node the inner node
   * @return the checked bits
   */
  @Handler
  public Map<Instruction, BitPattern> handle(SingleDecisionNode node) {

    var result = new LinkedHashMap<Instruction, BitPattern>();

    if (!node.isMatch()) {

      // If this is not a match, we cannot mark any bits as 'checked'.
      if (node.getOtherChild() == null) {
        return result;
      }

      final Map<Instruction, BitPattern> otherChildren = node.getOtherChild().accept(this);
      if (otherChildren == null) {
        throw new IllegalStateException("Expected non-matching child entries to exist");
      }
      return otherChildren;
    }

    final Map<Instruction, BitPattern> matchingChildren = node.getMatchingChild().accept(this);
    if (matchingChildren == null) {
      throw new IllegalStateException("Expected matching child entries to exist");
    }

    for (var child : matchingChildren.entrySet()) {

      var pattern = node.getPattern();
      var childPattern = child.getValue();

      result.put(child.getKey(), combinePatterns(pattern, childPattern));
    }

    if (node.getOtherChild() == null) {
      return result;
    }

    final Map<Instruction, BitPattern> otherChildren = node.getOtherChild().accept(this);
    if (otherChildren == null) {
      throw new IllegalStateException("Expected non-matching child entries to exist");
    }

    // We cannot consider these bits checked, since they can be anything except the pattern.
    result.putAll(otherChildren);

    return result;

  }
}
