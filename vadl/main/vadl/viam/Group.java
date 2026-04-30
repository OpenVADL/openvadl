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
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

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
  public abstract static sealed class Expression implements WithLocation {
    private final SourceLocation location;

    public Expression(SourceLocation location) {
      this.location = location;
    }

    public abstract <R> R accept(GroupVisitor<R> visitor);

    @Override
    public SourceLocation location() {
      return location;
    }
  }

  /**
   * An expression literal.
   */
  public static final class Literal extends Expression {

    private final Operation operation;

    public Literal(WithLocation node, Operation operation) {
      super(node.location());
      this.operation = operation;
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
      return visitor.visit(this);
    }

    public Operation op() {
      return operation;
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
  public static final class Sequence extends Expression {
    private final List<Expression> elems;

    public Sequence(WithLocation node, List<Expression> elems) {
      super(node.location());
      this.elems = elems;
    }

    public List<Expression> elements() {
      return elems;
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
      return visitor.visit(this);
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
  public static final class Alternation extends Expression {
    private final List<Expression> elements;

    public Alternation(WithLocation node, List<Expression> elements) {
      super(node.location());
      this.elements = elements;
    }

    public List<Expression> elements() {
      return elements;
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
      return visitor.visit(this);
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
  public static final class Repetition extends Expression {

    private final Expression expr;
    private final Constant.Value from;
    private final Constant.Value to;

    /**
     * Creates a repetition expression.
     *
     * @param node       the source node of the repetition expression
     * @param expression the expression to be repeated
     * @param from       the minimum number of repetitions
     * @param to         the maximum number of repetitions
     */
    public Repetition(WithLocation node, Expression expression, Constant.Value from,
                      Constant.Value to) {
      super(node.location());
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
    public <R> R accept(GroupVisitor<R> visitor) {
      return visitor.visit(this);
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
  public static final class Permutation extends Expression {

    private final List<Expression> elements;

    public Permutation(WithLocation node, List<Expression> elements) {
      super(node.location());
      this.elements = elements;
    }

    public List<Expression> elements() {
      return elements;
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
      return visitor.visit(this);
    }

    @Override
    public String toString() {
      return "Permutation["
          + "elems=" + elements + ']';
    }

  }

  /**
   * Visitor over the extended regular expression nodes.
   *
   * @param <R> the result type.
   */
  public interface GroupVisitor<R> {
    R visit(Literal literal);

    R visit(Sequence sequence);

    R visit(Alternation alternation);

    R visit(Permutation permutation);

    R visit(Repetition repetition);
  }

}
