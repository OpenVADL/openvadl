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

public sealed interface MacroCall extends WithLocation
    permits MacroCall.Definition, MacroCall.Statement, MacroCall.Expr, MacroCall.Node {

  static MacroCall of(Identifier name, List<String> subcalls, List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType,
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

  default Identifier name() {
    return switch (this) {
      case Node node -> node.name;
      case Definition definition -> definition.name;
      case Statement statement -> statement.name;
      case Expr expr -> expr.name;
    };
  }

  default List<String> subcalls() {
    return switch (this) {
      case Node node -> node.subcalls;
      case Definition definition -> definition.subcalls;
      case Statement statement -> statement.subcalls;
      case Expr expr -> expr.subcalls;
    };
  }

  default List<vadl.ast.nodes.Node> arguments() {
    return switch (this) {
      case Node node -> node.arguments;
      case Definition definition -> definition.arguments;
      case Statement statement -> statement.arguments;
      case Expr expr -> expr.arguments;
    };
  }

  default SyntaxType syntaxType() {
    return switch (this) {
      case Node node -> node.syntaxType;
      case Definition definition -> definition.syntaxType;
      case Statement statement -> statement.syntaxType;
      case Expr expr -> expr.syntaxType;
    };
  }


  final class Definition extends vadl.ast.nodes.Definition implements MacroCall {

    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    public Definition(Identifier name, List<String> subcalls,
                      List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType, SourceLocation sourceLocation) {
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
      // TOOD
      throw new UnsupportedOperationException();
    }
  }

  final class Statement extends vadl.ast.nodes.Statement implements MacroCall {

    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    public Statement(Identifier name, List<String> subcalls,
                     List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType, SourceLocation sourceLocation) {
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
      // TODO
      throw new UnsupportedOperationException();
    }
  }

  final class Expr extends vadl.ast.nodes.Expr implements MacroCall, IdentifierOrPlaceholder, IsId {
    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    public Expr(Identifier name, List<String> subcalls, List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType,
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
      // TODO
      throw new UnsupportedOperationException();
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


  final class Node extends vadl.ast.nodes.Node implements MacroCall, IsBinOp, IsUnOp, IsEncs {

    Identifier name;
    List<String> subcalls;
    List<vadl.ast.nodes.Node> arguments;
    SyntaxType syntaxType;
    SourceLocation sourceLocation;

    public Node(Identifier name, List<String> subcalls,
                List<vadl.ast.nodes.Node> arguments, SyntaxType syntaxType, SourceLocation sourceLocation) {
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
      // TOOD
      throw new UnsupportedOperationException();
    }

  }
}
