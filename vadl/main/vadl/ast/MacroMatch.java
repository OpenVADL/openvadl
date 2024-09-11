package vadl.ast;

import java.util.List;
import vadl.utils.SourceLocation;

sealed interface IsMacroMatch
    permits MacroMatchNode, MacroMatchExpr, MacroMatchStatement, MacroMatchDefinition {
}

record MacroMatch(SyntaxType resultType, List<Choice> choices, Node defaultChoice,
                  SourceLocation sourceLocation) {

  void prettyPrint(int indent, StringBuilder sb) {
    sb.append("  ".repeat(indent)).append("match : ").append(resultType.print()).append("(");
    for (Choice choice : choices) {
      sb.append("\n").append("  ".repeat(indent + 1));
      choice.candidate.prettyPrint(indent + 1, sb);
      if (choice.comparison == Comparison.EQUAL) {
        sb.append(" = ");
      } else {
        sb.append(" != ");
      }
      choice.match.prettyPrint(indent + 1, sb);
      sb.append(" => ");
      choice.result.prettyPrint(indent + 1, sb);
      sb.append(" ;");
    }
    sb.append("\n").append(" ".repeat(indent + 1));
    sb.append("_ => ");
    defaultChoice.prettyPrint(indent + 1, sb);
    sb.append("\n").append(" ".repeat(indent)).append(")\n");
  }

  enum Comparison { EQUAL, NOT_EQUAL }

  record Choice(Node candidate, Comparison comparison, Node match, Node result) {
  }
}
