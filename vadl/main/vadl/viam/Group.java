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

package vadl.viam;

import java.util.List;

/**
 * Represents a VADL operation group, which is an extended regular expression over
 * {@link Operation}s.
 */
public class Group extends Definition {

  private final Expression expression;

  public Group(Identifier identifier, Expression expression) {
    super(identifier);
    this.expression = expression;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  public Expression getExpression() {
    return expression;
  }

  sealed public interface Expression {
    <U> U accept(ExpressionVisitor<U> visitor);
  }

  public record Literal(Operation op) implements Expression {
    @Override
    public <U> U accept(ExpressionVisitor<U> visitor) {
      return visitor.visit(this);
    }
  }

  public record Sequence(List<Expression> elems) implements Expression {
    @Override
    public <U> U accept(ExpressionVisitor<U> visitor) {
      return visitor.visit(this);
    }
  }

  public record Alternation(List<Expression> elems) implements Expression {
    @Override
    public <U> U accept(ExpressionVisitor<U> visitor) {
      return visitor.visit(this);
    }
  }

  public record Repetition(Expression expr, Constant.Value from, Constant.Value to)
      implements Expression {
    @Override
    public <U> U accept(ExpressionVisitor<U> visitor) {
      return visitor.visit(this);
    }
  }

  public record Permutation(List<Expression> elems) implements Expression {
    @Override
    public <U> U accept(ExpressionVisitor<U> visitor) {
      return visitor.visit(this);
    }
  }

  public interface ExpressionVisitor<U> {
    U visit(Literal lit);

    U visit(Sequence seq);

    U visit(Alternation alt);

    U visit(Repetition rep);

    U visit(Permutation perm);
  }

}
