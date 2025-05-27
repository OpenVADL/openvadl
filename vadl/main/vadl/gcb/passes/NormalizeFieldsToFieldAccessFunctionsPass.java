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

package vadl.gcb.passes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * It is possible to use fields in an {@link Instruction#behavior()}.
 * However, they are missing encoding and predicate which makes the lowering
 * harder in the compiler backends. This pass replaces {@link FieldRefNode} in the
 * behavior by freshly created {@link FieldAccessRefNode}.
 */
public class NormalizeFieldsToFieldAccessFunctionsPass extends Pass {
  public NormalizeFieldsToFieldAccessFunctionsPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("NormalizeFieldsToFieldAccessFunctionsPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var registerIndices =
        (HashSet<FieldRefNode>) passResults.lastResultOf(
            DetectRegisterIndicesPass.class);

    var machineInstructions = viam.isa().stream().flatMap(isa -> isa.ownInstructions().stream());
    var pseudoInstructions = viam
        .isa().stream().flatMap(isa -> isa.ownPseudoInstructions().stream())
        .flatMap(pseudoInstruction -> pseudoInstruction.behavior().getNodes(InstrCallNode.class))
        .map(InstrCallNode::target);

    Stream.concat(machineInstructions, pseudoInstructions)
        .forEach(instruction -> {
          // We assume that every FieldRefNode has to be lifted to a field access function
          // when it is not register indices.
          var immediates =
              getImmediatesWithoutFieldAccessFunction(instruction.behavior(), registerIndices);

          immediates
              .forEach(fieldRefNode -> {
                var id = fieldRefNode.formatField().identifier.append("generated");
                var fieldAccess = new Format.FieldAccess(
                    id,
                    createAccessFunction(id, fieldRefNode),
                    null, // can be automatically generated
                    createPredicateFunction(id, fieldRefNode)
                );
                var fieldAccessRefNode = new FieldAccessRefNode(
                    fieldAccess,
                    fieldRefNode.type()
                );
                fieldRefNode.replaceAndDelete(fieldAccessRefNode);
              });
        });

    return null;
  }

  /**
   * Get the immediates of {@code behavior} which are not in {@code registerIndices} and not
   * referenced by at least one {@link FieldRefNode}.
   */
  private List<FieldRefNode> getImmediatesWithoutFieldAccessFunction(
      Graph behavior,
      Set<FieldRefNode> registerIndices) {
    var fieldRefNodesInFieldAccessFunctions = behavior.getNodes(FieldAccessRefNode.class)
        .flatMap(fieldAccessRefNode -> fieldAccessRefNode.fieldAccess().fieldRefs().stream())
        .collect(Collectors.toSet());

    return behavior.getNodes(FieldRefNode.class)
        // First, it has not to be register index.
        .filter(fieldRefNode -> !registerIndices.contains(fieldRefNode))
        // Second, it has not to be part of a field access function.
        .filter(fieldRefNode -> !fieldRefNodesInFieldAccessFunctions.contains(
            fieldRefNode.formatField()))
        .toList();
  }

  private Function createAccessFunction(Identifier fieldRefId, FieldRefNode fieldRefNode) {
    var id = fieldRefId.append("accessFunction");
    var graph = new Graph(id.lower());
    ControlNode endNode = graph.addWithInputs(new ReturnNode(fieldRefNode.copy()));
    endNode.setSourceLocation(fieldRefNode.location());
    ControlNode startNode = graph.add(new StartNode(endNode));
    startNode.setSourceLocation(fieldRefNode.location());
    return new Function(id, new Parameter[] {}, fieldRefNode.type(), graph);
  }


  private Function createPredicateFunction(Identifier fieldRefId, FieldRefNode fieldRefNode) {
    var id = fieldRefId.append("predicateFunction");
    var graph = new Graph(id.lower());
    ControlNode endNode =
        graph.addWithInputs(new ReturnNode(new ConstantNode(Constant.Value.fromBoolean(true))));
    endNode.setSourceLocation(fieldRefNode.location());
    ControlNode startNode = graph.add(new StartNode(endNode));
    startNode.setSourceLocation(fieldRefNode.location());
    return new Function(id, new Parameter[] {}, Type.bool(), graph);
  }
}
