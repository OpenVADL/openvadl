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

package vadl.viam.graph.control;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;


/**
 * The call to an instruction used in {@link vadl.viam.PseudoInstruction} definitions.
 * As the order of such instruction calls is well-defined, this node is a {@link ControlNode}
 * with exactly one successor node.
 *
 * <p>The {@code paramFields} are a list of {@link Format.Field}s that are required to be set
 * for the {@code target} {@link Instruction}. The {@code arguments} is a list of expression
 * nodes that are associated to the {@code paramFields}, such as the types must match.</p>
 *
 * <p>A VADL might look like this
 * <pre>
 * {@code pseudo instruction MOV( rd : Bits<5>, rs1 : Bits<5> ) = {
 *     ADDI{ rd = rd, rs1 = rs1, imm = 0 }
 * }}
 * </pre>
 * with a call to {@code ADDI}.</p>
 */
public class InstrCallNode extends DirectionalNode {

  @DataValue
  protected Instruction target;
  @DataValue
  protected List<Format.Field> paramFields;

  @Input
  protected NodeList<ExpressionNode> arguments;


  /**
   * Constructs an InstrCallNode object with the given paramFields and arguments.
   *
   * @param target      the instruction that is getting called
   * @param paramFields the list of Format.Field objects that are required to be set for the
   *                    target Instruction
   * @param arguments   the list of ExpressionNode objects that are associated to the paramFields
   */
  public InstrCallNode(Instruction target, List<Format.Field> paramFields,
                       NodeList<ExpressionNode> arguments) {
    this.target = target;
    this.paramFields = paramFields;
    this.arguments = arguments;
  }

  public Instruction target() {
    return target;
  }

  public void setTarget(Instruction instruction) {
    this.target = instruction;
  }

  public List<Format.Field> getParamFields() {
    return paramFields;
  }

  public NodeList<ExpressionNode> arguments() {
    return arguments;
  }

  public ExpressionNode getArgument(Format.Field field) {
    var index = paramFields.indexOf(field);
    return arguments.get(index);
  }

  @Override
  public void verifyState() {
    ensure(paramFields.size() == arguments.size(),
        "Parameter fields and arguments do not match");
    for (var arg : arguments) {
      arg.ensure(arg.type() instanceof DataType,
          "Instruction Call arguments must have a DataType type, but got %s", arg.type());
    }
    ensure(
        IntStream.range(0, paramFields.size() - 1)
            .allMatch(
                i -> ((DataType) arguments.get(i).type()).isTrivialCastTo(
                    paramFields.get(i).type())),
        "Parameter fields do not match concrete argument fields"
    );
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(target);
    collection.add(paramFields);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(arguments);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arguments = arguments.stream().map(e ->
            visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  public Node copy() {
    return new InstrCallNode(target, paramFields,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()));
  }

  @Override
  public Node shallowCopy() {
    return new InstrCallNode(target, paramFields, arguments);
  }
}
