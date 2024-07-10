package vadl.ast;

import java.util.List;


record Macro(Identifier name, List<MacroParam> params, Node body, SyntaxType returnType) {

}

record MacroParam(Identifier name, SyntaxType type) {
}



