package vadl.ast;

import java.util.List;
import java.util.Map;

record Macro(Identifier name, List<MacroParam> params, Node body, SyntaxType returnType,
             Map<String, Node> boundArguments)
    implements MacroOrPlaceholder {
}

record MacroParam(Identifier name, SyntaxType type) {
}

interface MacroInstance {
  MacroOrPlaceholder macroOrPlaceholder();
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
