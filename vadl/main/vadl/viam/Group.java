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
import java.util.Objects;

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

  /**
   * An extended regular expression node of a VADL group definition.
   */
  public sealed interface Expression {
    void accept(DefinitionVisitor visitor);
  }

  /**
   * An expression literal.
   */
  public static final class Literal extends Definition implements Expression {

    private final Operation operation;

    public Literal(Identifier identifier, Operation operation) {
      super(identifier);
      this.operation = operation;
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    public Operation op() {
      return operation;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Literal) obj;
      return Objects.equals(this.operation, that.operation);
    }

    @Override
    public int hashCode() {
      return Objects.hash(operation);
    }

    @Override
    public String toString() {
      return "Literal["
          + "op=" + operation + ']';
    }

  }

  /**
   * A sequence expression.
   */
  public static final class Sequence extends Definition implements Expression {
    private final List<Expression> elems;

    public Sequence(Identifier identifier, List<Expression> elems) {
      super(identifier);
      this.elems = elems;
    }

    public List<Expression> elements() {
      return elems;
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Sequence) obj;
      return Objects.equals(this.elems, that.elems);
    }

    @Override
    public int hashCode() {
      return Objects.hash(elems);
    }

    @Override
    public String toString() {
      return "Sequence["
          + "elems=" + elems + ']';
    }

  }

  /**
   * An alternation expression.
   */
  public static final class Alternation extends Definition implements Expression {
    private final List<Expression> elements;

    public Alternation(Identifier identifier, List<Expression> elements) {
      super(identifier);
      this.elements = elements;
    }

    public List<Expression> elements() {
      return elements;
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Alternation) obj;
      return Objects.equals(this.elements, that.elements);
    }

    @Override
    public int hashCode() {
      return Objects.hash(elements);
    }

    @Override
    public String toString() {
      return "Alternation["
          + "elems=" + elements + ']';
    }

  }

  /**
   * A repetition expression.
   */
  public static final class Repetition extends Definition implements Expression {

    private final Expression expr;
    private final Constant.Value from;
    private final Constant.Value to;

    /**
     * Creates a repetition expression.
     *
     * @param identifier the identifier of the repetition expression
     * @param expression the expression to be repeated
     * @param from       the minimum number of repetitions
     * @param to         the maximum number of repetitions
     */
    public Repetition(Identifier identifier, Expression expression, Constant.Value from,
                      Constant.Value to) {
      super(identifier);
      this.expr = expression;
      this.from = from;
      this.to = to;
    }

    public Expression expression() {
      return expr;
    }

    public Constant.Value from() {
      return from;
    }

    public Constant.Value to() {
      return to;
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Repetition) obj;
      return Objects.equals(this.expr, that.expr)
          && Objects.equals(this.from, that.from)
          && Objects.equals(this.to, that.to);
    }

    @Override
    public int hashCode() {
      return Objects.hash(expr, from, to);
    }

    @Override
    public String toString() {
      return "Repetition["
          + "expr=" + expr + ", "
          + "from=" + from + ", "
          + "to=" + to + ']';
    }

  }

  /**
   * A permutation expression.
   */
  public static final class Permutation extends Definition implements Expression {

    private final List<Expression> elements;

    public Permutation(Identifier identifier, List<Expression> elements) {
      super(identifier);
      this.elements = elements;
    }

    public List<Expression> elements() {
      return elements;
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Permutation) obj;
      return Objects.equals(this.elements, that.elements);
    }

    @Override
    public int hashCode() {
      return Objects.hash(elements);
    }

    @Override
    public String toString() {
      return "Permutation["
          + "elems=" + elements + ']';
    }

  }

}
