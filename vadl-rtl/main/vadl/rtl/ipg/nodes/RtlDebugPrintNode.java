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

package vadl.rtl.ipg.nodes;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Node to print a debug message in RTL simulation.
 */
public class RtlDebugPrintNode extends SideEffectNode implements RtlConditionalNode {

  private static final String PLACEHOLDER = "%[a-z]";

  @DataValue
  protected String formatString;

  @Input
  protected NodeList<ExpressionNode> values;

  /**
   * Create new debug print node.
   *
   * @param condition print only when this is true
   * @param formatString string with <code>%[a-z]</code> as placeholders
   * @param values values to replace the placeholders with
   */
  public RtlDebugPrintNode(@Nullable ExpressionNode condition, String formatString,
                           NodeList<ExpressionNode> values) {
    super(condition);
    this.formatString = formatString;
    this.values = values;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    render((s, n) -> ""); // try to render, checks the format string
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formatString);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    values = values.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(values);
  }

  /**
   * Renders the format string by mapping the value inputs to a string that replaces the placeholder
   * in the format string.
   *
   * @param renderValue mapping function to replace placeholders, takes placeholder and expression
   *                    node and returns a string.
   * @return rendered string
   */
  public String render(BiFunction<String, ExpressionNode, String> renderValue) {
    var matcher = Pattern.compile(PLACEHOLDER).matcher(formatString);
    var iter = values.iterator();
    var result = matcher.replaceAll(res -> {
      ensure(iter.hasNext(), "Not enough values for format string");
      return renderValue.apply(res.group(), iter.next())
          .replace("\\", "\\\\")
          .replace("$", "\\$");
    });
    ensure(!iter.hasNext(), "Too many values for format string");
    return result;
  }

  @Override
  public Node copy() {
    return new RtlDebugPrintNode(
        (condition != null) ? condition.copy() : null, formatString, values.copy());
  }

  @Override
  public Node shallowCopy() {
    return new RtlDebugPrintNode(condition, formatString, values);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public Node asNode() {
    return this;
  }
}
