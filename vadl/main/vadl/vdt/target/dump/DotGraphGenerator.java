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
import static vadl.vdt.target.dump.DotGraphGeneratorDispatcher.dispatch;
import static vadl.vdt.utils.BitVectorUtils.fittingPowerOfTwo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.utils.Pair;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.impl.regular.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.model.impl.LeafNodeImpl;
import vadl.vdt.target.common.dto.DecisionTreeStatistics;

/**
 * Generates a simple dot graph from a VDT.
 */
@DispatchFor(value = Node.class, include = {"vadl.vdt"}, returnType = Pair.class)
public class DotGraphGenerator implements Visitor<Pair<Integer, List<CharSequence>>> {

  private final Node tree;
  private final DecisionTreeStatistics stats;
  private final AtomicInteger counter = new AtomicInteger();

  /**
   * Construct the DOT graph generator.
   *
   * @param tree The vadl decode tree.
   */
  public DotGraphGenerator(Node tree) {
    this.tree = tree;
    this.stats = statistics(tree);
  }

  /**
   * Generate a dot graph from the given decode tree.
   *
   * @return the dot graph
   */
  public CharSequence generate() {
    // Reset the counter
    counter.set(0);

    // Write the header
    var sb = new StringBuilder();
    sb.append("digraph G {\n");
    sb.append("    rankdir=TB;\n");
    sb.append("    node [shape=box];\n");
    sb.append("\n");

    // Recursively generate the graph
    var result = tree.accept(this);

    // Append the result lines
    if (result != null) {
      result.right().forEach(sb::append);
    }

    sb.append("}\n");
    return sb;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Pair<Integer, List<CharSequence>> visit(InnerNode node) {
    return (Pair<Integer, List<CharSequence>>) dispatch(this, node);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Pair<Integer, List<CharSequence>> visit(LeafNode node) {
    return (Pair<Integer, List<CharSequence>>) dispatch(this, node);
  }

  /**
   * Handle graph generation for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the node id and the list of lines to add to the graph
   */
  @Handler
  public Pair<Integer, List<CharSequence>> handle(InnerNodeImpl node) {
    var id = counter.getAndIncrement();

    final List<CharSequence> lines = new ArrayList<>();
    lines.add("    %d [label=\"Mask 0x%x\"];\n".formatted(id, node.getMask().toValue()));

    // Handle default node
    if (node.getFallback() != null) {
      var childResult = node.getFallback().accept(this);
      if (childResult != null) {
        lines.addAll(childResult.right());
        lines.add("    %d -> %d [label=\"default\"];\n".formatted(id, childResult.left()));
      }
    }

    // Handle other children
    node.getChildren().forEach((pattern, child) -> {

      var childResult = child.accept(this);
      if (childResult == null) {
        return;
      }

      lines.addAll(childResult.right());
      lines.add("    %d -> %d [label=\"0x%x\"];\n".formatted(id, childResult.left(),
          pattern.toBitVector().toValue()));
    });

    return Pair.of(id, lines);
  }

  /**
   * Handle graph generation for {@link LeafNodeImpl}.
   *
   * @param node the leaf node
   * @return the node id and the list of lines to add to the graph
   */
  @Handler
  public Pair<Integer, List<CharSequence>> handle(LeafNodeImpl node) {
    var id = counter.getAndIncrement();
    var name = node.instruction().source().simpleName();
    var leafNode = "    %d [label=\"%s\"];\n".formatted(id, name);
    return Pair.of(id, List.of(leafNode));
  }

  /**
   * Handler for {@link MultiDecisionNode}.
   *
   * @param node the inner node
   * @return the text representations
   */
  @Handler
  public Pair<Integer, List<CharSequence>> handle(MultiDecisionNode node) {

    var id = counter.getAndIncrement();

    final List<CharSequence> lines = new ArrayList<>();

    final int insnWidth = fittingPowerOfTwo(stats.getMaxInstructionWidth());

    final BigInteger mask = node.getMask().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();

    int shift = insnWidth - (node.getOffset() + length);
    if (offset > 0 && shift > 0) {
      lines.add("    %d [label=\"(insn >> %d) & 0x%x\"];\n".formatted(id, shift, mask));
    } else {
      lines.add("    %d [label=\"Mask 0x%x\"];\n".formatted(id, mask));
    }

    // Handle switch cases
    node.getChildren().forEach((pattern, child) -> {

      var childResult = child.accept(this);
      if (childResult == null) {
        return;
      }

      lines.addAll(childResult.right());
      lines.add("    %d -> %d [label=\"0x%x\"];\n".formatted(id, childResult.left(),
          pattern.toBitVector().toValue()));
    });

    return Pair.of(id, lines);
  }

  /**
   * Handler for {@link SingleDecisionNode}.
   *
   * @param node the inner node
   * @return the text representations
   */
  @Handler
  public Pair<Integer, List<CharSequence>> handle(SingleDecisionNode node) {
    var id = counter.getAndIncrement();

    final List<CharSequence> lines = new ArrayList<>();

    final int insnWidth = fittingPowerOfTwo(stats.getMaxInstructionWidth());

    final BigInteger mask = node.getPattern().toMaskVector().toValue();
    final BigInteger value = node.getPattern().toBitVector().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();

    int shift = insnWidth - (node.getOffset() + length);
    if (offset > 0 && shift > 0) {
      lines.add(
          "    %d [label=\"(insn >> %d) & 0x%x == 0x%x\"];\n".formatted(id, shift, mask, value));
    } else {
      lines.add("    %d [label=\"insn & 0x%x == 0x%x\"];\n".formatted(id, mask, value));
    }

    // Handle if/else case
    var matchingResult = node.getMatchingChild().accept(this);
    if (matchingResult != null) {
      lines.addAll(matchingResult.right());
      lines.add("    %d -> %d [label=\"Yes\"];\n".formatted(id, matchingResult.left()));
    }

    var otherResult = node.getOtherChild().accept(this);
    if (otherResult != null) {
      lines.addAll(otherResult.right());
      lines.add("    %d -> %d [label=\"No\"];\n".formatted(id, otherResult.left()));
    }

    return Pair.of(id, lines);
  }

}
