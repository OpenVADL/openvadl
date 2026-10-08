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

package vadl.ast.nodes;


import java.util.List;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

/**
 * A macro call represents any call to a macro, or macro parameter, it also allows field access of
 * records.
 *
 * <p>Examples:
 * <pre>{@code
 * $ABC
 * $ABC()
 * $ABC(hello; world)
 * $ABC.XYZ
 * $ABC.XYZ(hello;world)
 * }</pre>
 *
 * <p>Since they can be injected into the AST at different points, a Expression, Statement,
 * Defintion and generic Node version are available. All of them are constructed via
 * {@link MacroCall#of(Identifier, List, List, SyntaxType, SourceLocation)}.
 *
 * <p>This node and it's backing structures should never leave the parser!
 */
public sealed interface MacroCall extends WithLocation
    permits MacroCall.Definition, MacroCall.Statement, MacroCall.Expr, MacroCall.Node {

  /** Construct a MacroCall, it will be backed based by the syntax type provided.
   *
   * @param name of the macro that will be called or parameter that is invoked.
   * @param subcalls that access a field in a record, or potentially follow a record chain.
   * @param arguments with which the macro is called.
   * @param syntaxType which the macro will produce/return when envoked.
   * @param location of the call.
   * @return the newly constructed call.
   */
  static MacroCall of(Identifier name, List<String> subcalls, List<vadl.ast.nodes.Node> arguments,
                      SyntaxType syntaxType,
                      SourceLocation location) {
    if (syntaxType.isSubTypeOf(BasicSyntaxType.ISA_DEFS)) {
      return new MacroCall.Definition(name, subcalls, arguments, syntaxType,
          location);
    } else if (syntaxType.isSubTypeOf(BasicSyntaxType.STATS)) {
      return new MacroCall.Statement(name, subcalls, arguments, syntaxType,
          location);
    } else if (syntaxType.isSubTypeOf(BasicSyntaxType.EX)) {
      return new MacroCall.Expr(name, subcalls, arguments, syntaxType, location);
    } else {
      return new MacroCall.Node(name, subcalls, arguments, syntaxType, location);
    }
  }

  /**
   * The name of then macro called.
   *
   * @return the name.
   */
  default Identifier name() {
    return switch (this) {
      case Node node -> node.name;
      case Definition definition -> definition.name;
      case Statement statement -> statement.name;
      case Expr expr -> expr.name;
    };
  }

  /**
   * The subcalls of the macro called.
   *
   * @return the subcalls.
   */
  default List<String> subcalls() {
    return switch (this) {
      case Node node -> node.subcalls;
      case Definition definition -> definition.subcalls;
      case Statement statement -> statement.subcalls;
      case Expr expr -> expr.subcalls;
    };
  }

  /**
   * The arguments of the macro call.
   *
   * @return the arguments.
   */
  default List<vadl.ast.nodes.Node> arguments() {
    return switch (this) {
      case Node node -> node.arguments;
      case Definition definition -> definition.arguments;
      case Statement statement -> statement.arguments;
      case Expr expr -> expr.arguments;
    };
  }

  /**
   * The syntax type the macro call will produce.
   *
   * @return the type.
   */
  default SyntaxType syntaxType() {
    return switch (this) {
      case Node node -> node.syntaxType;
      case Definition definition -> definition.syntaxType;
      case Statement statement -> statement.syntaxType;
      case Expr expr -> expr.syntaxType;
    };
  }


  /**
   * The definition backing variant of the MacroCall.
   * This node should never leave the parser!
   */
  final class Definition extends vadl.ast.nodes.Definition implements MacroCall {

    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    private Definition(Identifier name, List<String> subcalls,
                       List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType,
                       SourceLocation sourceLocation) {
      this.name = name;
      this.subcalls = subcalls;
      this.arguments = arguments;
      this.syntaxType = syntaxType;
      this.sourceLocation = sourceLocation;
    }

    @Override
    public <R> R accept(DefinitionVisitor<R> visitor) {
      return visitor.visit(this);
    }

    @Override
    public SourceLocation location() {
      return sourceLocation;
    }

    @Override
    public SyntaxType syntaxType() {
      return syntaxType;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      // No need to implement this will always be expanded before pretty printing.
      throw new IllegalStateException("This shouldn't be reachable");
    }
  }

  /**
   * The statement backing variant of the MacroCall.
   * This node should never leave the parser!
   */
  final class Statement extends vadl.ast.nodes.Statement implements MacroCall {

    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    private Statement(Identifier name, List<String> subcalls,
                      List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType,
                      SourceLocation sourceLocation) {
      this.name = name;
      this.subcalls = subcalls;
      this.arguments = arguments;
      this.syntaxType = syntaxType;
      this.sourceLocation = sourceLocation;
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
      return visitor.visit(this);
    }

    @Override
    public SourceLocation location() {
      return sourceLocation;
    }

    @Override
    public SyntaxType syntaxType() {
      return syntaxType;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      // No need to implement this will always be expanded before pretty printing.
      throw new IllegalStateException("This shouldn't be reachable");
    }
  }

  /**
   * The expression backing variant of the MacroCall.
   * This node should never leave the parser!
   */
  final class Expr extends vadl.ast.nodes.Expr implements MacroCall, IdentifierOrPlaceholder, IsId {
    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    private Expr(Identifier name, List<String> subcalls, List<vadl.ast.nodes.Node> arguments,
                 SyntaxType syntaxType,
                 SourceLocation sourceLocation) {
      this.name = name;
      this.subcalls = subcalls;
      this.arguments = arguments;
      this.syntaxType = syntaxType;
      this.sourceLocation = sourceLocation;
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
      return visitor.visit(this);
    }

    @Override
    public SourceLocation location() {
      return sourceLocation;
    }

    @Override
    public SyntaxType syntaxType() {
      return syntaxType;
    }

    @Override
    public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
      builder.append(name.name);
      for (String subcall : subcalls) {
        builder.append(".");
        builder.append(subcall);
      }
      if (!arguments.isEmpty()) {
        builder.append("(");
        boolean first = true;
        for (var arg : arguments) {
          if (!first) {
            builder.append(", ");
          }
          arg.prettyPrint(indent, builder);
        }
        builder.append(")");
      }
    }

    @Override
    public String pathToString() {
      var sb = new StringBuilder();
      prettyPrint(0, sb);
      return sb.toString();
    }

    @Nullable
    @Override
    public vadl.ast.nodes.Node target() {
      return null;
    }
  }


  /**
   * The definition backing variant of the MacroCall.
   * This node should never leave the parser!
   */
  final class Node extends vadl.ast.nodes.Node implements MacroCall, IsBinOp, IsUnOp, IsEncs {

    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    private Node(Identifier name, List<String> subcalls,
                 List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType,
                 SourceLocation sourceLocation) {
      this.name = name;
      this.subcalls = subcalls;
      this.arguments = arguments;
      this.syntaxType = syntaxType;
      this.sourceLocation = sourceLocation;
    }

    @Override
    public SourceLocation location() {
      return sourceLocation;
    }

    @Override
    public SyntaxType syntaxType() {
      return syntaxType;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      // No need to implement this will always be expanded before pretty printing.
      throw new IllegalStateException("This shouldn't be reachable");
    }

  }
}
