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

package vadl.viam.passes.statusBuiltInInlinePass;

import static java.util.Objects.requireNonNull;
import static vadl.utils.GraphUtils.equ;
import static vadl.utils.GraphUtils.testSignBit;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;

abstract class Inliner {

  BuiltInCall builtInCall;

  Graph graph;

  @Nullable
  private TupleGetFieldNode resultUser;
  @Nullable
  private TupleGetFieldNode zeroUser;
  @Nullable
  private TupleGetFieldNode carryUser;
  @Nullable
  private TupleGetFieldNode overflowUser;
  @Nullable
  private TupleGetFieldNode negativeUser;

  @Nullable
  private ExpressionNode resultNode;
  @Nullable
  private ExpressionNode zeroNode;
  @Nullable
  private ExpressionNode carryNode;
  @Nullable
  private ExpressionNode overflowNode;
  @Nullable
  private ExpressionNode negativeNode;

  Inliner(BuiltInCall builtInCall) {
    this.builtInCall = builtInCall;
    this.graph = requireNonNull(builtInCall.graph());
    initUsers(builtInCall);
  }

  final void inline() {
    inline(resultUser, this::getResult);
    inline(zeroUser, this::getZero);
    inline(carryUser, this::getCarry);
    inline(overflowUser, this::getOverflow);
    inline(negativeUser, this::getNegative);
  }

  abstract ExpressionNode createResult();

  abstract ExpressionNode checkOverflow();

  abstract ExpressionNode checkCarry();

  ExpressionNode checkZero() {
    var result = getResult();
    return equ(
        result,
        Constant.Value.fromInteger(BigInteger.ZERO, result.type().asDataType())
            .toNode()
    );
  }

  ExpressionNode checkNegative() {
    var result = getResult();
    return testSignBit(result);
  }

  final ExpressionNode getResult() {
    if (resultNode == null) {
      resultNode = createResult();
    }
    return resultNode;
  }

  final ExpressionNode getZero() {
    if (zeroNode == null) {
      zeroNode = checkZero();
    }
    return zeroNode;
  }

  final ExpressionNode getCarry() {
    if (carryNode == null) {
      carryNode = checkCarry();
    }
    return carryNode;
  }

  final ExpressionNode getOverflow() {
    if (overflowNode == null) {
      overflowNode = checkOverflow();
    }
    return overflowNode;
  }

  final ExpressionNode getNegative() {
    if (negativeNode == null) {
      negativeNode = checkNegative();
    }
    return negativeNode;
  }

  protected ExpressionNode firstArg() {
    return builtInCall.arguments().get(0);
  }

  protected ExpressionNode secondArg() {
    return builtInCall.arguments().get(1);
  }

  protected ExpressionNode thirdArg() {
    return builtInCall.arguments().get(2);
  }

  protected ExpressionNode binaryOf(BuiltInTable.BuiltIn builtIn) {
    return BuiltInCall.of(builtIn, firstArg(), secondArg());
  }

  private void inline(@Nullable TupleGetFieldNode user, Supplier<ExpressionNode> creator) {
    if (user != null) {
      user.replaceAndDelete(creator.get());
    }
  }

  private void initUsers(BuiltInCall builtInCall) {

    // from a given node that represents the status tuple, we get all users of the status fields
    // and assign them to the class' fields.
    Consumer<DependencyNode> initStatusUsers = (DependencyNode node) -> {
      node.usages().forEach(usage -> {
        if (usage instanceof TupleGetFieldNode getField) {
          switch (getField.index()) {
            case 0 -> negativeUser = getField;
            case 1 -> zeroUser = getField;
            case 2 -> carryUser = getField;
            case 3 -> overflowUser = getField;
            default -> throw new ViamGraphError("User of status tuple accesses non existing index.")
                .addContext(getField)
                .addContext("status tuple", node);
          }
        }
      });
    };


    builtInCall.usages().filter(TupleGetFieldNode.class::isInstance)
        .map(TupleGetFieldNode.class::cast)
        .forEach(node -> {
          switch (node.index()) {
            case 0 -> resultUser = node;
            case 1 -> initStatusUsers.accept(node);
            default -> throw new ViamGraphError("User accesses non existing index.")
                .addContext(node)
                .addContext("built-in call", builtInCall);
          }
        });
  }

}
