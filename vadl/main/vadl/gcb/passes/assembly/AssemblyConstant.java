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
   * Get {@link TokenKind} based on constant.
   */
  public static TokenKind getKind(Constant.Str constant) {
    switch (constant.toString()) {
      case " ":
        return TokenKind.Space;
      case "+":
        return TokenKind.Plus;
      case "-":
        return TokenKind.Minus;
      case "*":
        return TokenKind.Star;
      case "/":
        return TokenKind.Slash;
      case "\\":
        return TokenKind.BackSlash;
      case "?":
        return TokenKind.Question;
      case "!":
        return TokenKind.Exclaim;
      case ".":
        return TokenKind.Dot;
      case "$":
        return TokenKind.Dollar;
      case ",":
        return TokenKind.Comma;
      case "~":
        return TokenKind.Tilde;
      case ":":
        return TokenKind.Colon;
      case "^":
        return TokenKind.Caret;
      case "(":
        return TokenKind.LParen;
      case ")":
        return TokenKind.RParen;
      case "[":
        return TokenKind.LBrac;
      case "]":
        return TokenKind.RBrac;
      case "{":
        return TokenKind.LCurly;
      case "}":
        return TokenKind.RCurly;
      case "=":
        return TokenKind.Equal;
      case "==":
        return TokenKind.EqualEqual;
      case "!=":
        return TokenKind.ExclaimEqual;
      case "%":
        return TokenKind.Percent;
      case "#":
        return TokenKind.Hash;
      case "|":
        return TokenKind.Pipe;
      case "||":
        return TokenKind.PipePipe;
      case "&":
        return TokenKind.Amp;
      case "&&":
        return TokenKind.AmpAmp;
      case "@":
        return TokenKind.At;
      case "<":
        return TokenKind.Less;
      case "<=":
        return TokenKind.LessEqual;
      case "<<":
        return TokenKind.LessLess;
      case "<>":
        return TokenKind.LessGreater;
      case ">":
        return TokenKind.Greater;
      case ">=":
        return TokenKind.GreaterEqual;
      case "->":
        return TokenKind.MinusGreater;
      default:
        var pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        // Check if number
        if (pattern.matcher(constant.toString()).matches()) {
          return TokenKind.Integer;
        }
        return TokenKind.Identifier;
    }
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
    if (visitor instanceof AssemblyVisitor) {
      ((AssemblyVisitor) visitor).visit(this);
    } else {
      visitor.visit(this);
    }
  }

  public TokenKind kind() {
    return kind;
  }
}
