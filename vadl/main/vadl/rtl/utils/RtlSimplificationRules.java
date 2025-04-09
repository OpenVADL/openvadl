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

package vadl.rtl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AdditionWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AndWithFalseSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.AndWithTrueSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.DivisionWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.MultiplicationWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.MultiplicationWithZeroSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.OrWithFalseSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.OrWithTrueSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.RemainderWithOneSimplificationRule;
import vadl.viam.passes.algebraic_simplication.rules.impl.RemainderWithZeroSimplificationRule;

/**
 * Simplification rules for optimizations during the RTL generation.
 */
public class RtlSimplificationRules {

  public static final List<AlgebraicSimplificationRule> rules = new ArrayList<>();

  static {
    rules.add(new AdditionWithZeroSimplificationRule());
    rules.add(new MultiplicationWithZeroSimplificationRule());
    rules.add(new MultiplicationWithOneSimplificationRule());
    rules.add(new DivisionWithOneSimplificationRule());
    rules.add(new RemainderWithZeroSimplificationRule());
    rules.add(new RemainderWithOneSimplificationRule());
    rules.add(new AndWithFalseSimplificationRule());
    rules.add(new AndWithTrueSimplificationRule());
    rules.add(new OrWithTrueSimplificationRule());
    rules.add(new OrWithFalseSimplificationRule());
    rules.add(new AndWithZerosSimplificationRule());
    rules.add(new AndWithOnesSimplificationRule());
    rules.add(new OrWithOnesSimplificationRule());
    rules.add(new OrWithZerosSimplificationRule());
  }

  /**
   * Simplify when AND with zeros then return zeros.
   */
  public static class AndWithZerosSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof ExpressionNode n) {
        var matcher =
            new BuiltInMatcher(List.of(BuiltInTable.AND, BuiltInTable.ANDS),
                List.of(new AnyNodeMatcher(), new ConstantValueMatcher(
                    Constant.Value.of(0, n.type().asDataType()))));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.of(new ConstantNode(Constant.Value.of(0, n.type().asDataType())));
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify when AND with ones then return the other input.
   */
  public static class AndWithOnesSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof ExpressionNode n) {
        var ones = (1 << n.type().asDataType().bitWidth()) - 1;
        var matcher =
            new BuiltInMatcher(List.of(BuiltInTable.AND, BuiltInTable.ANDS),
                List.of(new AnyNodeMatcher(), new ConstantValueMatcher(
                    Constant.Value.of(ones, n.type().asDataType()))));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.ofNullable(n.inputs().toList().get(0));
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify when OR with ones then return ones.
   */
  public static class OrWithOnesSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof ExpressionNode n) {
        var ones = (1 << n.type().asDataType().bitWidth()) - 1;
        var matcher =
            new BuiltInMatcher(List.of(BuiltInTable.OR, BuiltInTable.ORS),
                List.of(new AnyNodeMatcher(), new ConstantValueMatcher(
                    Constant.Value.of(ones, n.type().asDataType()))));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.of(new ConstantNode(Constant.Value.of(ones, n.type().asDataType())));
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify when OR with zeros then return the other input.
   */
  public static class OrWithZerosSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof ExpressionNode n) {
        var matcher =
            new BuiltInMatcher(List.of(BuiltInTable.OR, BuiltInTable.ORS),
                List.of(new AnyNodeMatcher(), new ConstantValueMatcher(
                    Constant.Value.of(0, n.type().asDataType()))));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.ofNullable(n.inputs().toList().get(0));
        }
      }
      return Optional.empty();
    }
  }

}
