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
 * Generates a simple text tree representation of the VDT.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class TextGraphGenerator implements Visitor<List<StringBuilder>> {

  private final Node tree;
  private final DecisionTreeStatistics stats;

  /**
   * Construct the text graph generator.
   *
   * @param tree The vadl decode tree.
   */
  public TextGraphGenerator(Node tree) {
    this.tree = tree;
    this.stats = statistics(tree);
  }

  /**
   * Generate a text representation of the given tree.
   *
   * @return the text representation
   */
  public CharSequence generate() {

    var sb = new StringBuilder();

    var result = tree.accept(this);

    if (result != null) {
      result.forEach(l -> sb.append(l).append("\n"));
    }

    return sb;
  }

  @Nullable
  @Override
  public List<StringBuilder> visit(LeafNode node) {
    return List.of(new StringBuilder(node.instruction().source().simpleName()));
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public List<StringBuilder> visit(InnerNode node) {
    return (List<StringBuilder>) TextGraphGeneratorDispatcher.dispatch(this, node);
  }

  /**
   * Handler for {@link InnerNodeImpl}.
   *
   * @param node the inner node
   * @return the text representations
   */
  @Handler
  public List<StringBuilder> handle(InnerNodeImpl node) {
    var result = new ArrayList<StringBuilder>();

    var label = new StringBuilder();
    BigInteger mask = node.getMask().toValue();
    label.append("insn & 0x%x".formatted(mask));

    result.add(label);

    // Default
    Node defaultNode = node.getFallback();
    if (defaultNode != null) {
      var childLines = defaultNode.accept(this);
      if (childLines != null) {
        var defLabel = new StringBuilder("  |- default");
        result.add(defLabel);
        if (childLines.size() == 1) {
          defLabel.append(" -> ").append(childLines.getFirst());
        } else {
          childLines.stream().map(l -> l.insert(0, "  |  ")).forEach(result::add);
        }
      }
    }

    // Children
    for (var child : node.getChildren().entrySet()) {
      var childNode = child.getValue();

      var childLines = childNode.accept(this);
      if (childLines == null) {
        continue;

      }

      var edgeLabel = new StringBuilder("  |- ");
      edgeLabel.append("0x%x".formatted(child.getKey().toBitVector().toValue()));

      result.add(edgeLabel);

      if (childLines.size() == 1) {
        edgeLabel.append(" -> ").append(childLines.getFirst());
      } else {
        childLines.stream().map(l -> l.insert(0, "  |  ")).forEach(result::add);
      }
    }

    return result;
  }

  /**
   * Handler for {@link MultiDecisionNode}.
   *
   * @param node the inner node
   * @return the text representations
   */
  @Handler
  public List<StringBuilder> handle(MultiDecisionNode node) {

    var result = new ArrayList<StringBuilder>();

    final int insnWidth = fittingPowerOfTwo(stats.getMaxInstructionWidth());
    final BigInteger mask = node.getMask().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();

    var label = new StringBuilder();

    int shift = insnWidth - (node.getOffset() + length);
    if (offset > 0 && shift > 0) {
      label.append("(insn >> %d) & 0x%x".formatted(shift, mask));
    } else {
      label.append("insn & 0x%x".formatted(mask));
    }

    result.add(label);

    // Children
    for (var child : node.getChildren().entrySet()) {
      var childNode = child.getValue();

      var childLines = childNode.accept(this);
      if (childLines == null) {
        continue;

      }

      var edgeLabel = new StringBuilder("  |- ");
      edgeLabel.append("0x%x".formatted(child.getKey().toBitVector().toValue()));

      result.add(edgeLabel);

      if (childLines.size() == 1) {
        edgeLabel.append(" -> ").append(childLines.getFirst());
      } else {
        childLines.stream().map(l -> l.insert(0, "  |  ")).forEach(result::add);
      }
    }

    return result;
  }

  /**
   * Handler for {@link SingleDecisionNode}.
   *
   * @param node the inner node
   * @return the text representations
   */
  @Handler
  public List<StringBuilder> handle(SingleDecisionNode node) {

    var result = new ArrayList<StringBuilder>();

    final int insnWidth = fittingPowerOfTwo(stats.getMaxInstructionWidth());
    final BigInteger mask = node.getPattern().toMaskVector().toValue();
    final BigInteger value = node.getPattern().toBitVector().toValue();
    final int offset = node.getOffset();
    final int length = node.getLength();

    var label = new StringBuilder();

    int shift = insnWidth - (node.getOffset() + length);
    if (offset > 0 && shift > 0) {
      label.append("(insn >> %d) & 0x%x == 0x%x".formatted(shift, mask, value));
    } else {
      label.append("insn & 0x%x == 0x%x".formatted(mask, value));
    }

    result.add(label);

    // Handle if/else case
    var matchingResult = node.getMatchingChild().accept(this);
    if (matchingResult != null) {

      var edgeLabel = new StringBuilder("  |- True ");
      result.add(edgeLabel);

      if (matchingResult.size() == 1) {
        edgeLabel.append(" -> ").append(matchingResult.getFirst());
      } else {
        matchingResult.stream().map(l -> l.insert(0, "  |  ")).forEach(result::add);
      }
    }

    var otherResult = node.getOtherChild().accept(this);
    if (otherResult != null) {
      var edgeLabel = new StringBuilder("  |- False");
      result.add(edgeLabel);

      if (otherResult.size() == 1) {
        edgeLabel.append(" -> ").append(otherResult.getFirst());
      } else {
        otherResult.stream().map(l -> l.insert(0, "  |  ")).forEach(result::add);
      }
    }

    return result;
  }
}
