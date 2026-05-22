// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for working with group definitions.
 */
public interface GroupDefUtils {

  /**
   * Collect all operations occurring in a group expression.
   */
  class OperationCollector implements Group.GroupVisitor<List<OperationDefinition>> {

    /**
     * Collect all operations occurring in a group expression.
     *
     * @param group the group expression
     * @return the list of operations
     */
    public static List<OperationDefinition> operations(Group group) {
      return group.accept(new OperationCollector());
    }

    @Override
    public List<OperationDefinition> visit(Group.Sequence seq) {
      final var operations = new ArrayList<OperationDefinition>();
      for (Group group : seq.groups) {
        operations.addAll(group.accept(this));
      }
      return operations;
    }

    @Override
    public List<OperationDefinition> visit(Group.Alternative alt) {
      final var operations = new ArrayList<OperationDefinition>();
      for (Group.Sequence sequence : alt.sequences) {
        operations.addAll(sequence.accept(this));
      }
      return operations;
    }

    @Override
    public List<OperationDefinition> visit(Group.Permutation perm) {
      final var operations = new ArrayList<OperationDefinition>();
      for (Group.Sequence sequence : perm.sequences) {
        operations.addAll(sequence.accept(this));
      }
      return operations;
    }

    @Override
    public List<OperationDefinition> visit(Group.Literal lit) {
      return List.of(lit.getOperation());
    }

  }

  /**
   * Determine the maximum length of a group expression, i.e., the maximum number of literals that
   * can be matched by the group.
   */
  class GroupExprLengthCollector implements Group.GroupVisitor<Integer> {

    private final ConstantEvaluator constantEvaluator;

    public GroupExprLengthCollector(ConstantEvaluator constantEvaluator) {
      this.constantEvaluator = constantEvaluator;
    }

    public static int maxLength(ConstantEvaluator constantEvaluator, Group group) {
      return group.accept(new GroupExprLengthCollector(constantEvaluator));
    }

    @Override
    public Integer visit(Group.Sequence seq) {
      int len = 0;
      for (Group group : seq.groups) {
        len += group.accept(this);
      }
      return len;
    }

    @Override
    public Integer visit(Group.Alternative alt) {
      int len = 0;
      for (Group.Sequence seq : alt.sequences) {
        len = Math.max(len, seq.accept(this));
      }
      return len;
    }

    @Override
    public Integer visit(Group.Permutation perm) {
      int len = 0;
      for (Group.Sequence seq : perm.sequences) {
        len += seq.accept(this);
      }
      return len;
    }

    @Override
    public Integer visit(Group.Literal lit) {
      if (!(lit.size instanceof RangeExpr range)) {
        return 1;
      }
      final var constant = constantEvaluator.eval(range.to);
      return constant.value().intValueExact();
    }

  }
}
