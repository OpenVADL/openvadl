package vadl.ast;

import java.util.List;

record Macro(Identifier name, List<MacroParam> params, Node body, SyntaxType returnType)
    implements MacroOrPlaceholder {
}

record MacroParam(Identifier name, SyntaxType type) {
}

sealed interface MacroOrPlaceholder permits Macro, MacroPlaceholder {
  SyntaxType returnType();
}

record MacroPlaceholder(ProjectionType syntaxType, List<String> segments)
    implements MacroOrPlaceholder {
  @Override
  public SyntaxType returnType() {
    return syntaxType.resultType;
  }
}
