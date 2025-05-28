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

package vadl.viam.passes.functionInliner;

import static vadl.utils.GraphUtils.getSingleNode;

import com.google.common.collect.Streams;
import java.util.Arrays;
import vadl.utils.Pair;
import vadl.viam.Function;
import vadl.viam.Relocation;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * A common liner collection that holds the logic for all inliner passes.
 */
public class Inliner {

  /**
   * Inline all field accesses in the given behavior.
   */
  public static void inlineFieldAccess(Graph behavior) {
    var fieldAccesses = behavior.getNodes(FieldAccessRefNode.class)
        .toList();

    fieldAccesses.forEach(fieldAccessRefNode -> {
      var funcBehavior = fieldAccessRefNode.fieldAccess().accessFunction().behavior();
      var returnNode = getSingleNode(funcBehavior, ReturnNode.class);
      fieldAccessRefNode.replaceAndDelete(returnNode.value().copy());
    });
  }


  /**
   * Inline all functions in the given behavior.
   */
  public static void inlineFuncs(Graph behavior) {
    var functionCalls = behavior.getNodes(FuncCallNode.class)
        .filter(funcCallNode -> funcCallNode.function().behavior().isPureFunction())
        .filter(funcCallNode -> !(funcCallNode.function() instanceof Relocation))
        .toList();

    if (functionCalls.isEmpty()) {
      return;
    }

    functionCalls.forEach(functionCall -> {
      // replace the function call by a copy of the return value of the function
      functionCall.replaceAndDelete(
          inline(functionCall.function(), functionCall.arguments()));
    });

    // do it again until there are no more function calls
    inlineFuncs(behavior);
  }


  /**
   * Get an expression node representing the inlined value of the given function definition
   * with the given arguments.
   * The returned node and its dependencies are all uninitialized.
   *
   * @param function  that should be inlined
   * @param arguments the argument expressions that the function is called with
   * @return the inlined return value node (uninitialized)
   */
  public static ExpressionNode inline(Function function,
                                      NodeList<ExpressionNode> arguments) {
    // inline the all function calls (recursively)
    inlineFuncs(function.behavior());

    // copy function behavior
    var behaviorCopy = function.behavior().copy();


    // get return node of function behaviors
    var returnNode = getSingleNode(behaviorCopy, ReturnNode.class);

    // Replace every occurrence of `FuncParamNode` by a copy of the
    // given argument from the `FunctionCallNode`.
    Streams.zip(arguments.stream(),
            Arrays.stream(function.parameters()), Pair::new)
        .forEach(pair -> {
          var arg = pair.left().copy();
          var param = pair.right();
          var paramNodes = behaviorCopy.getNodes(FuncParamNode.class).toList();
          for (var paramNode : paramNodes) {
            if (paramNode.parameter() == param) {
              paramNode.replaceAndDelete(arg);
            }
          }
        });

    // replace the function call by a copy of the return value of the function
    return returnNode.value().copy();
  }

}
