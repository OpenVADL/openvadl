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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.rtl.ipg.nodes.OneHotDecodeNode;
import vadl.rtl.ipg.nodes.SelectByInstructionNode;
import vadl.types.BuiltInTable;
import vadl.utils.BigIntUtils;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantIntegerValueMatcher;
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
    rules.add(new RemainderWithOneSimplificationRule());
    rules.add(new AndWithFalseSimplificationRule());
    rules.add(new AndWithTrueSimplificationRule());
    rules.add(new OrWithTrueSimplificationRule());
    rules.add(new OrWithFalseSimplificationRule());
    rules.add(new AndWithZerosSimplificationRule());
    rules.add(new AndWithOnesSimplificationRule());
    rules.add(new OrWithOnesSimplificationRule());
    rules.add(new OrWithZerosSimplificationRule());
    rules.add(new SelectWithEqCasesSimplificationRule());
    rules.add(new SelectWithConstCondSimplificationRule());
    rules.add(new SelByInstrEqCasesSimplificationRule());
    rules.add(new SelByInstrConstSelSimplificationRule());
    rules.add(new OneHotConstInSimplificationRule());
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
                List.of(new AnyNodeMatcher(), new ConstantIntegerValueMatcher(BigInteger.ZERO)));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.of(Constant.Value.of(0, n.type().asDataType()).toNode());
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
        var ones = BigIntUtils.mask(n.type().asDataType().bitWidth(), 0);
        var matcher =
            new BuiltInMatcher(List.of(BuiltInTable.AND, BuiltInTable.ANDS),
                List.of(new AnyNodeMatcher(), new ConstantIntegerValueMatcher(ones)));

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
        var ones = BigIntUtils.mask(n.type().asDataType().bitWidth(), 0);
        var matcher =
            new BuiltInMatcher(List.of(BuiltInTable.OR, BuiltInTable.ORS),
                List.of(new AnyNodeMatcher(), new ConstantIntegerValueMatcher(ones)));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.of(Constant.Value.fromInteger(ones, n.type().asDataType()).toNode());
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
                List.of(new AnyNodeMatcher(), new ConstantIntegerValueMatcher(BigInteger.ZERO)));

        var matchings = TreeMatcher.matches(Stream.of(node), matcher);
        if (!matchings.isEmpty()) {
          return Optional.ofNullable(n.inputs().toList().get(0));
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify select with equal cases.
   */
  public static class SelectWithEqCasesSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof SelectNode n && n.trueCase() == n.falseCase()) {
        return Optional.of(n.trueCase());
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify select with constant condition.
   */
  public static class SelectWithConstCondSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof SelectNode n && n.condition() instanceof ConstantNode c) {
        if (c.constant().asVal().bool()) {
          return Optional.of(n.trueCase());
        } else {
          return Optional.of(n.falseCase());
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify select-by-instruction nodes with equal cases.
   */
  public static class SelByInstrEqCasesSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof SelectByInstructionNode n && !n.values().isEmpty()) {
        // check if all values are equal
        var first = n.values().get(0);
        if (n.values().stream().allMatch(first::equals)) {
          return Optional.of(first);
        }
        // optimize values (merges equal values)
        var values = new HashSet<>(n.values());
        if (values.size() < n.values().size()) {
          for (ExpressionNode value : values) {
            if (n.values().stream().filter(value::equals).count() > 1) {
              // by removing and re-adding the value
              var ins = n.remove(value);
              ins.forEach(i -> n.add(i, value));
            }
          }
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify select-by-instruction nodes with constant selection input.
   */
  public static class SelByInstrConstSelSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof SelectByInstructionNode n) {
        var selInput = n.selection();
        if (selInput instanceof ConstantNode c) {
          var i = c.constant().asVal().intValue();
          if (i >= 0 && i < n.values().size()) {
            return Optional.of(n.values().get(i));
          }
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Simplify one-hot-decode nodes with constant inputs.
   */
  public static class OneHotConstInSimplificationRule implements AlgebraicSimplificationRule {
    @Override
    public Optional<Node> simplify(Node node) {
      if (node instanceof OneHotDecodeNode n) {
        Integer sel = null;
        for (ExpressionNode value : n.values()) {
          if (value instanceof ConstantNode c) {
            if (c.constant().asVal().bool()) {
              if (sel != null) {
                return Optional.empty(); // ignore encoding error
              }
              sel = n.values().indexOf(value);
            }
          } else {
            return Optional.empty();
          }
        }
        if (sel != null) {
          return Optional.of(Constant.Value.of(sel, n.type().asDataType()).toNode());
        }
      }
      return Optional.empty();
    }
  }

}
