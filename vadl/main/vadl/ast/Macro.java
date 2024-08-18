package vadl.ast;

import java.util.List;

sealed interface MacroOrPlaceholder permits Macro, MacroPlaceholder {
  SyntaxType returnType();
}

record Macro(Identifier name, List<MacroParam> params, Node body, SyntaxType returnType)
    implements MacroOrPlaceholder {
}

record MacroParam(Identifier name, SyntaxType type) {
}

record MacroPlaceholder(ProjectionType syntaxType, List<String> segments)
    implements MacroOrPlaceholder {
  @Override
  public SyntaxType returnType() {
    return syntaxType.resultType;
  }
}
