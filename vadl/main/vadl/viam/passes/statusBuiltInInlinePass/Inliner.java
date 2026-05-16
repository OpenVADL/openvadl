// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import static vadl.types.BuiltInTable.BUILTIN_RESULT;
import static vadl.types.BuiltInTable.BUILTIN_STATUS;
import static vadl.types.StatusType.CARRY;
import static vadl.types.StatusType.NEGATIVE;
import static vadl.types.StatusType.OVERFLOW;
import static vadl.types.StatusType.ZERO;
import static vadl.utils.GraphUtils.equ;
import static vadl.utils.GraphUtils.getUsagesByUnrollingLets;
import static vadl.utils.GraphUtils.testSignBit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.StructGetFieldNode;

/**
 * The inliner is responsible
 * for replacing a specific kind of status built-in by non-status built-ins.
 * E.g. the {@link vadl.viam.passes.statusBuiltInInlinePass.ArithmeticInliner.AddS} inliner
 * replaces a {@link BuiltInTable#ADDS} built-in call.
 *
 * <p>An inliner must implement three methods: {@link #createResult()}, {@link #checkOverflow()},
 * {@link #checkCarry()}.
 * The {@link #checkNegative()} and {@link #checkZero()} status flags are already implemented
 * by this super class.
 * Those check methods return the expression tree that implements the respective check.
 * This super class takes care of only create checks when required (i.e., the status flag is used
 * by the callee).</p>
 */
abstract class Inliner {

  BuiltInCall builtInCall;

  Graph graph;

  @Nullable
  private StructGetFieldNode resultUser;
  private final List<StructGetFieldNode> zeroUsers = new ArrayList<>();
  private final List<StructGetFieldNode> carryUsers = new ArrayList<>();
  private final List<StructGetFieldNode> overflowUsers = new ArrayList<>();
  private final List<StructGetFieldNode> negativeUsers = new ArrayList<>();

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

  public void inline() {
    inline(resultUser, this::getResult);
    inlineAll(zeroUsers, this::getZero);
    inlineAll(carryUsers, this::getCarry);
    inlineAll(overflowUsers, this::getOverflow);
    inlineAll(negativeUsers, this::getNegative);
  }

  private void inline(@Nullable StructGetFieldNode user, Supplier<ExpressionNode> creator) {
    if (user != null) {
      user.replaceAndDelete(creator.get());
    }
  }

  private void inlineAll(List<StructGetFieldNode> users, Supplier<ExpressionNode> creator) {
    if (users.isEmpty()) {
      return;
    }

    var replacement = creator.get();
    var first = true;
    for (var user : users) {
      user.replaceAndDelete(first ? replacement : replacement.copy());
      first = false;
    }
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

  protected ExpressionNode arg0() {
    return builtInCall.arguments().get(0);
  }

  protected ExpressionNode arg1() {
    return builtInCall.arguments().get(1);
  }

  protected ExpressionNode arg2() {
    return builtInCall.arguments().get(2);
  }

  protected ExpressionNode binaryOf(BuiltInTable.BuiltIn builtIn) {
    return BuiltInCall.of(builtIn, arg0(), arg1());
  }

  private void initUsers(BuiltInCall builtInCall) {

    // from a given node that represents the status struct, we get all users of the status fields
    // and assign them to the class' fields.
    Consumer<ExpressionNode> initStatusUsers = (ExpressionNode node) -> {
      getUsagesByUnrollingLets(node).forEach(usage -> {
        if (usage instanceof StructGetFieldNode getField) {
          switch (getField.field()) {
            case NEGATIVE -> negativeUsers.add(getField);
            case ZERO -> zeroUsers.add(getField);
            case CARRY -> carryUsers.add(getField);
            case OVERFLOW -> overflowUsers.add(getField);
            default -> throw new ViamGraphError("User of status accesses non existing field.")
                .addContext(getField)
                .addContext("status", node);
          }
        }
      });
    };


    getUsagesByUnrollingLets(builtInCall)
        .filter(StructGetFieldNode.class::isInstance)
        .map(StructGetFieldNode.class::cast)
        .forEach(node -> {
          switch (node.field()) {
            case BUILTIN_RESULT -> resultUser = node;
            case BUILTIN_STATUS -> initStatusUsers.accept(node);
            default -> throw new ViamGraphError("User accesses non existing field.")
                .addContext(node)
                .addContext("built-in call", builtInCall);
          }
        });
  }

}
