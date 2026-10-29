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

package vadl.vdt.impl.irregular.vliw.utils;

import java.util.LinkedHashSet;
import java.util.Set;
import vadl.viam.Group;
import vadl.viam.Group.Alternation;
import vadl.viam.Group.GroupVisitor;
import vadl.viam.Group.Literal;
import vadl.viam.Group.Permutation;
import vadl.viam.Group.Repetition;
import vadl.viam.Group.Sequence;

/**
 * Collect all literals occurring in a given group expression.
 */
public class LiteralsCollector implements GroupVisitor<Set<Literal>> {

  /**
   * Return all literals mentioned in the given expression.
   *
   * @param group The group definition.
   * @return The literals.
   */
  public static Set<Literal> getLiterals(Group group) {
    return group.getExpression().accept(new LiteralsCollector());
  }

  @Override
  public Set<Literal> visit(Literal literal) {
    return Set.of(literal);
  }

  @Override
  public Set<Literal> visit(Sequence sequence) {
    final Set<Literal> literals = new LinkedHashSet<>();
    for (Group.Expression e : sequence.elements()) {
      literals.addAll(e.accept(this));
    }
    return literals;
  }

  @Override
  public Set<Literal> visit(Alternation alternation) {
    final Set<Literal> literals = new LinkedHashSet<>();
    for (Group.Expression e : alternation.elements()) {
      literals.addAll(e.accept(this));
    }
    return literals;
  }

  @Override
  public Set<Literal> visit(Permutation permutation) {
    final Set<Literal> literals = new LinkedHashSet<>();
    for (Group.Expression e : permutation.elements()) {
      literals.addAll(e.accept(this));
    }
    return literals;
  }

  @Override
  public Set<Literal> visit(Repetition repetition) {
    return repetition.expression().accept(this);
  }
}
