package vadl.gcb.passes.assemblyConstantIntern;

import java.util.regex.Pattern;
import org.thymeleaf.util.NumberUtils;
import vadl.gcb.passes.assemblyConstantIntern.visitors.AssemblyVisitor;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * A predefined node for the {@link Assembly#function()#graph()}.
 */
public class AssemblyConstant extends ConstantNode {


  public enum TOKEN_KIND {
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

  private final TOKEN_KIND kind;

  public AssemblyConstant(Constant.Str constant) {
    super(constant);
    this.kind = getKind(constant);
  }

  public static TOKEN_KIND getKind(Constant.Str constant) {
    switch (constant.toString()) {
      case " ":
        return TOKEN_KIND.Space;
      case "+":
        return TOKEN_KIND.Plus;
      case "-":
        return TOKEN_KIND.Minus;
      case "*":
        return TOKEN_KIND.Star;
      case "/":
        return TOKEN_KIND.Slash;
      case "\\":
        return TOKEN_KIND.BackSlash;
      case "?":
        return TOKEN_KIND.Question;
      case "!":
        return TOKEN_KIND.Exclaim;
      case ".":
        return TOKEN_KIND.Dot;
      case "$":
        return TOKEN_KIND.Dollar;
      case ",":
        return TOKEN_KIND.Comma;
      case "~":
        return TOKEN_KIND.Tilde;
      case ":":
        return TOKEN_KIND.Colon;
      case "^":
        return TOKEN_KIND.Caret;
      case "(":
        return TOKEN_KIND.LParen;
      case ")":
        return TOKEN_KIND.RParen;
      case "[":
        return TOKEN_KIND.LBrac;
      case "]":
        return TOKEN_KIND.RBrac;
      case "{":
        return TOKEN_KIND.LCurly;
      case "}":
        return TOKEN_KIND.RCurly;
      case "=":
        return TOKEN_KIND.Equal;
      case "==":
        return TOKEN_KIND.EqualEqual;
      case "!=":
        return TOKEN_KIND.ExclaimEqual;
      case "%":
        return TOKEN_KIND.Percent;
      case "#":
        return TOKEN_KIND.Hash;
      case "|":
        return TOKEN_KIND.Pipe;
      case "||":
        return TOKEN_KIND.PipePipe;
      case "&":
        return TOKEN_KIND.Amp;
      case "&&":
        return TOKEN_KIND.AmpAmp;
      case "@":
        return TOKEN_KIND.At;
      case "<":
        return TOKEN_KIND.Less;
      case "<=":
        return TOKEN_KIND.LessEqual;
      case "<<":
        return TOKEN_KIND.LessLess;
      case "<>":
        return TOKEN_KIND.LessGreater;
      case ">":
        return TOKEN_KIND.Greater;
      case ">=":
        return TOKEN_KIND.GreaterEqual;
      case "->":
        return TOKEN_KIND.MinusGreater;
      default:
        var pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        // Check if number
        if (pattern.matcher(constant.toString()).matches()) {
          return TOKEN_KIND.Integer;
        }
        return TOKEN_KIND.Identifier;
    }
  }

  @Override
  public Node copy() {
    return new AssemblyConstant((Constant.Str) constant);
  }

  @Override
  public Node shallowCopy() {
    return new AssemblyConstant((Constant.Str) constant);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((AssemblyVisitor) visitor).visit(this);
  }

  public TOKEN_KIND kind() {
    return kind;
  }
}
