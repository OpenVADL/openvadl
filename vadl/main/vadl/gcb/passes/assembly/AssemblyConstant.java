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

package vadl.gcb.passes.assembly;

import java.util.regex.Pattern;
import vadl.gcb.passes.assembly.visitors.AssemblyVisitor;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * A predefined node for the assembly's {@link Function#behavior()}.
 */
public class AssemblyConstant extends ConstantNode {

  /**
   * LLVM's set of predefined tokens.
   */
  public enum TokenKind {
    // Markers
    Eof, Error,

    // String values.
    Identifier,

    // Integer values.
    Integer,
    EndOfStatement,
    Colon,
    Space,
    Plus, Minus, Tilde,
    Slash,     // '/'
    BackSlash, // '\'
    LParen, RParen, LBrac, RBrac, LCurly, RCurly,
    Question, Star, Dot, Comma, Dollar, Equal, EqualEqual,

    Pipe, PipePipe, Caret,
    Amp, AmpAmp, Exclaim, ExclaimEqual, Percent, Hash,
    Less, LessEqual, LessLess, LessGreater,
    Greater, GreaterEqual, GreaterGreater, At, MinusGreater,
  }

  private final TokenKind kind;

  public AssemblyConstant(Constant.Str constant) {
    super(constant);
    this.kind = getKind(constant);
  }

  /**
   * Get the string representation of a constant.
   */
  public String string() {
    return switch (this.kind) {
      case Space -> " ";
      case Plus -> "+";
      case Minus -> "-";
      case Star -> "*";
      case Slash -> "/";
      case BackSlash -> "\\";
      case Question -> "?";
      case Exclaim -> "!";
      case Dot -> ".";
      case Dollar -> "$";
      case Comma -> ",";
      case Tilde -> "~";
      case Colon -> ":";
      case Caret -> "^";
      case LParen -> "(";
      case RParen -> ")";
      case LBrac -> "[";
      case RBrac -> "]";
      case LCurly -> "{";
      case RCurly -> "}";
      case Equal -> "=";
      case EqualEqual -> "==";
      case ExclaimEqual -> "!=";
      case Percent -> "%";
      case Hash -> "#";
      case Pipe -> "|";
      case PipePipe -> "||";
      case Amp -> "&";
      case AmpAmp -> "&&";
      case At -> "@";
      case Less -> "<";
      case LessEqual -> "<=";
      case LessLess -> "<<";
      case LessGreater -> "<>";
      case Greater -> ">";
      case GreaterEqual -> ">=";
      case MinusGreater -> "->";
      default -> constant.toString();
    };
  }

  /**
   * Get {@link TokenKind} based on constant.
   */
  public static TokenKind getKind(Constant.Str constant) {
    return switch (constant.toString()) {
      case " " -> TokenKind.Space;
      case "+" -> TokenKind.Plus;
      case "-" -> TokenKind.Minus;
      case "*" -> TokenKind.Star;
      case "/" -> TokenKind.Slash;
      case "\\" -> TokenKind.BackSlash;
      case "?" -> TokenKind.Question;
      case "!" -> TokenKind.Exclaim;
      case "." -> TokenKind.Dot;
      case "$" -> TokenKind.Dollar;
      case "," -> TokenKind.Comma;
      case "~" -> TokenKind.Tilde;
      case ":" -> TokenKind.Colon;
      case "^" -> TokenKind.Caret;
      case "(" -> TokenKind.LParen;
      case ")" -> TokenKind.RParen;
      case "[" -> TokenKind.LBrac;
      case "]" -> TokenKind.RBrac;
      case "{" -> TokenKind.LCurly;
      case "}" -> TokenKind.RCurly;
      case "=" -> TokenKind.Equal;
      case "==" -> TokenKind.EqualEqual;
      case "!=" -> TokenKind.ExclaimEqual;
      case "%" -> TokenKind.Percent;
      case "#" -> TokenKind.Hash;
      case "|" -> TokenKind.Pipe;
      case "||" -> TokenKind.PipePipe;
      case "&" -> TokenKind.Amp;
      case "&&" -> TokenKind.AmpAmp;
      case "@" -> TokenKind.At;
      case "<" -> TokenKind.Less;
      case "<=" -> TokenKind.LessEqual;
      case "<<" -> TokenKind.LessLess;
      case "<>" -> TokenKind.LessGreater;
      case ">" -> TokenKind.Greater;
      case ">=" -> TokenKind.GreaterEqual;
      case "->" -> TokenKind.MinusGreater;
      default -> {
        var pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        // Check if number
        if (pattern.matcher(constant.toString()).matches()) {
          yield TokenKind.Integer;
        }
        yield TokenKind.Identifier;
      }
    };
  }

  @Override
  public ExpressionNode copy() {
    return new AssemblyConstant((Constant.Str) constant);
  }

  @Override
  public Node shallowCopy() {
    return new AssemblyConstant((Constant.Str) constant);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof AssemblyVisitor asmVisitor) {
      asmVisitor.visit(this);
    } else {
      visitor.visit(this);
    }
  }

  public TokenKind kind() {
    return kind;
  }
}
