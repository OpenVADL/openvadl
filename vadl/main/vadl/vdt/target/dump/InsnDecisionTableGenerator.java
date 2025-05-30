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

package vadl.vdt.target.dump;

import static vadl.vdt.target.common.DecisionTreeStatsCalculator.statistics;
import static vadl.vdt.utils.BitVectorUtils.fittingPowerOfTwo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
import vadl.vdt.target.common.dto.DecisionTreeStatistics;

/**
 * Generates a simple text table to list the decision path for each instruction.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class InsnDecisionTableGenerator implements Visitor<List<List<CharSequence>>> {

  private final Node tree;
  private final DecisionTreeStatistics stats;

  /**
   * Construct the decision table generator.
   *
   * @param tree The vadl decode tree.
   */
  public InsnDecisionTableGenerator(Node tree) {
    this.tree = tree;
    this.stats = statistics(tree);
  }

  /**
   * Generates the path table for the given tree.
   *
   * @return the path table (a list of columns) or an empty list if the tree is empty
   */
  public List<List<CharSequence>> generate() {
    var rows = tree.accept(this);
    if (rows == null) {
      return List.of();
    }

    // Transform: Rows -> Columns (required for the dump table enricher)
    var table = new ArrayList<List<CharSequence>>();
    for (int i = 0; i < rows.size(); i++) {
      var row = rows.get(i);
      for (int j = 0; j < row.size(); j++) {
        if (table.size() <= j) {
          table.add(new ArrayList<>());
        }
        while (table.get(j).size() < i) {
          table.get(j).add("");
        }
        table.get(j).add(i, row.get(j));
      }
    }

    if (table.isEmpty()) {
      return table;
    }

    // Add a header to each column
    table.getFirst().addFirst("Instruction");
    for (int i = 1; i < table.size(); i++) {
      table.get(i).addFirst("DL" + (i - 1));
    }

    return table;
  }

  @Nullable
  @Override
  public List<List<CharSequence>> visit(LeafNode node) {
    return List.of(new ArrayList<>(List.of(node.instruction().source().simpleName())));
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public List<List<CharSequence>> visit(InnerNode node) {
    return (List<List<CharSequence>>) InsnDecisionTableGeneratorDispatcher.dispatch(this, node);
  }

  /**
   * Handler for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the path table
   */
  @Handler
  public List<List<CharSequence>> handle(InnerNodeImpl node) {

    var label = new StringBuilder();
    label.append("insn & 0x%x".formatted(node.getMask().toValue()));

    var result = new ArrayList<List<CharSequence>>();

    // Default
    var defaultNode = node.getFallback();
    if (defaultNode != null) {
      var childLines = defaultNode.accept(this);
      if (childLines != null) {
        childLines.forEach(l -> l.add(1, label + " (default)"));
      }
      result.addAll(childLines);
    }

    // Decisions
    for (var entry : node.getChildren().entrySet()) {

      var childLines = entry.getValue().accept(this);
      if (childLines == null) {
        continue;
      }

      var childLabel = "%s == 0x%x".formatted(label, entry.getKey().toBitVector().toValue());
      childLines.forEach(l -> l.add(1, childLabel));
      result.addAll(childLines);
    }

    return result;
  }

  /**
   * Handler for {@link MultiDecisionNode}.
   *
   * @param node the inner node
   * @return the path table
   */
  @Handler
  public List<List<CharSequence>> handle(MultiDecisionNode node) {

    var label = new StringBuilder();

    final int insnWidth = fittingPowerOfTwo(stats.getMaxInstructionWidth());
    final BigInteger mask = node.getMask().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();
    int shift = insnWidth - (node.getOffset() + length);

    if (offset > 0 && shift > 0) {
      label.append("(insn >> %d) & 0x%x".formatted(shift, mask));
    } else {
      label.append("insn & 0x%x".formatted(mask));
    }

    var result = new ArrayList<List<CharSequence>>();

    // Decisions
    for (var entry : node.getChildren().entrySet()) {

      var childLines = entry.getValue().accept(this);
      if (childLines == null) {
        continue;
      }

      var childLabel = "%s == 0x%x".formatted(label, entry.getKey().toBitVector().toValue());
      childLines.forEach(l -> l.add(1, childLabel));
      result.addAll(childLines);
    }

    return result;
  }

  /**
   * Handler for {@link SingleDecisionNode}.
   *
   * @param node the inner node
   * @return the path table
   */
  @Handler
  public List<List<CharSequence>> handle(SingleDecisionNode node) {

    var label = new StringBuilder();

    final int insnWidth = fittingPowerOfTwo(stats.getMaxInstructionWidth());
    final BigInteger mask = node.getPattern().toMaskVector().toValue();
    final BigInteger value = node.getPattern().toBitVector().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();
    int shift = insnWidth - (node.getOffset() + length);

    if (offset > 0 && shift > 0) {
      label.append("(insn >> %d) & 0x%x".formatted(shift, mask));
    } else {
      label.append("insn & 0x%x".formatted(mask));
    }

    var result = new ArrayList<List<CharSequence>>();

    // If/Else cases
    var matchingLines = node.getMatchingChild().accept(this);
    if (matchingLines != null) {
      var matchingLabel = "%s == 0x%x".formatted(label, value);
      matchingLines.forEach(l -> l.add(1, matchingLabel));
      result.addAll(matchingLines);
    }

    var otherLines = node.getOtherChild().accept(this);
    if (otherLines != null) {
      var otherLabel = "%s != 0x%x".formatted(label, value);
      otherLines.forEach(l -> l.add(1, otherLabel));
      result.addAll(otherLines);
    }

    return result;
  }
}
