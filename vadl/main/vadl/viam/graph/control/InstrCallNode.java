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

import com.google.common.collect.Streams;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.utils.Either;
import vadl.utils.Pair;
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
 * <p>The {@code paramFieldsOrAccesses} are a list of either {@link Format.Field}s or
 * {@link Format.FieldAccess}es that are required to be set for the {@code target}
 * {@link Instruction}. The {@code arguments} is a list of expression nodes that are associated
 * to the {@code paramFields}, such as the types must match.</p>
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
  protected List<Either<Format.Field, Format.FieldAccess>> paramFieldsOrAccesses;

  @Input
  protected NodeList<ExpressionNode> arguments;


  /**
   * Constructs an InstrCallNode object with the given paramFields and arguments.
   *
   * @param target                the instruction that is getting called
   * @param paramFieldsOrAccesses the list of Format.Field or Format.FieldAccess objects that are
   *                              required to be set for the target Instruction
   * @param arguments             the list of ExpressionNode objects that are associated
   *                              to the paramFields
   */
  public InstrCallNode(Instruction target,
                       List<Either<Format.Field, Format.FieldAccess>> paramFieldsOrAccesses,
                       NodeList<ExpressionNode> arguments) {
    this.target = target;
    this.paramFieldsOrAccesses = paramFieldsOrAccesses;
    this.arguments = arguments;
  }

  public Instruction target() {
    return target;
  }

  public void setTarget(Instruction instruction) {
    this.target = instruction;
  }

  /**
   * Returns the list of {@link Format.Field}s that are required to be set for the target
   * {@link Instruction}. If a parameter is a {@link Format.FieldAccess},
   * its referenced {@link Format.Field} is returned.
   *
   * @return the list of {@link Format.Field}s
   */
  public List<Format.Field> getParamFields() {
    return paramFieldsOrAccesses.stream()
        .map(paramField -> paramField.isLeft() ? paramField.left() : paramField.right().fieldRef())
        .toList();
  }

  public List<Either<Format.Field, Format.FieldAccess>> getParamFieldsOrAccesses() {
    return paramFieldsOrAccesses;
  }

  public NodeList<ExpressionNode> arguments() {
    return arguments;
  }

  /**
   * Get a zipped stream for parameter and argument.
   */
  public Stream<Pair<Either<Format.Field, Format.FieldAccess>,
      ExpressionNode>> getZippedArgumentsWithParameters() {
    return Streams.zip(getParamFieldsOrAccesses().stream(), arguments.stream(), Pair::of);
  }

  public ExpressionNode getArgument(Format.Field field) {
    var index = getParamFields().indexOf(field);
    return arguments.get(index);
  }

  /**
   * Check if the parameter corresponding to a given field is a {@link Format.FieldAccess}.
   *
   * @param field the given field
   * @return true if the parameter is a {@link Format.FieldAccess}, false otherwise
   */
  public boolean isParameterFieldAccess(Format.Field field) {
    return paramFieldsOrAccesses.stream().anyMatch(
        paramField -> paramField.isRight() && paramField.right().fieldRef().equals(field));
  }

  /**
   * Extract the {@link Format.FieldAccess} from {@code paramFieldsOrAccesses}.
   */
  public List<Format.FieldAccess> usedFieldAccesses() {
    return paramFieldsOrAccesses
        .stream()
        .filter(Either::isRight)
        .map(Either::right)
        .toList();
  }

  @Override
  public void verifyState() {
    ensure(paramFieldsOrAccesses.size() == arguments.size(),
        "Parameter fields and arguments do not match");
    for (var arg : arguments) {
      arg.ensure(arg.type() instanceof DataType,
          "Instruction Call arguments must have a DataType type, but got %s", arg.type());
    }
    ensure(IntStream.range(0, paramFieldsOrAccesses.size() - 1).allMatch(
            i -> arguments.get(i).type().isTrivialCastTo(getParamFields().get(i).type())),
        "Parameter fields do not match concrete argument fields");
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(target);
    collection.add(paramFieldsOrAccesses);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(arguments);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arguments = arguments.stream().map(e -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  public Node copy() {
    return new InstrCallNode(target, paramFieldsOrAccesses,
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()));
  }

  @Override
  public Node shallowCopy() {
    return new InstrCallNode(target, paramFieldsOrAccesses, arguments);
  }
}
