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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.vdt.impl.theiling.InnerNodeImpl;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;

/**
 * Generates a simple text table to list the decision path for each instruction.
 */
@DispatchFor(value = InnerNode.class, include = {"vadl.vdt"}, returnType = List.class)
public class InsnDecisionTableGenerator implements Visitor<List<List<CharSequence>>> {

  /**
   * Generates the path table for the given tree.
   *
   * @param tree the tree
   * @return the path table (a list of columns) or an empty list if the tree is empty
   */
  public static List<List<CharSequence>> generate(Node tree) {
    var rows = tree.accept(new InsnDecisionTableGenerator());
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
    table.get(0).add(0, "Instruction");
    for (int i = 1; i < table.size(); i++) {
      table.get(i).add(0, "DL" + (i - 1));
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
    label.append("insn & 0x").append(node.getMask().toValue().toString(16));

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
}
