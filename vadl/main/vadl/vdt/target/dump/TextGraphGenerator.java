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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.NotImplementedException;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.impl.regular.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;

/**
 * Generates a simple text tree representation of the VDT.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class TextGraphGenerator implements Visitor<List<StringBuilder>> {

  /**
   * Generate a text representation of the given tree.
   *
   * @param tree the tree
   * @return the text representation
   */
  public static CharSequence generate(Node tree) {

    var sb = new StringBuilder();

    var result = tree.accept(new TextGraphGenerator());

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
    label.append("insn & 0x").append(mask.toString(16));

    result.add(label);

    // Default
    Node defaultNode = node.getFallback();
    if (defaultNode != null) {
      var childLines = defaultNode.accept(this);
      if (childLines != null) {
        result.add(new StringBuilder("  |- default"));
        childLines.stream().map(l -> l.insert(0, "|  ")).forEach(result::add);
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
      edgeLabel.append("0x").append(child.getKey().toBitVector().toValue().toString(16));

      result.add(edgeLabel);

      if (childLines.size() == 1) {
        edgeLabel.append(" -> ").append(childLines.get(0));
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
    throw new NotImplementedException("Not implemented");
  }

  @Handler
  public List<StringBuilder> handle(SingleDecisionNode node) {
    throw new NotImplementedException("Not implemented");
  }
}
