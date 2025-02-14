package vadl.lcb.codegen.assembly;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.common.AsmParserFunctionCodeGenerator;
import vadl.cppCodeGen.common.PureFunctionCodeGenerator;
import vadl.cppCodeGen.context.CAsmContext;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.viam.Function;
import vadl.viam.ViamError;
import vadl.viam.asm.AsmToken;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignTo;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGrammarElement;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmLocalVarUse;
import vadl.viam.asm.elements.AsmOption;
import vadl.viam.asm.elements.AsmRepetition;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.AsmStringLiteralUse;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;

/**
 * Generates the cpp code for assembly parsing from the assembly description grammar.
 */
@DispatchFor(
    value = AsmGrammarElement.class,
    context = CAsmContext.class,
    include = "vadl.viam.asm"
)
@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
public class AssemblyParserCodeGenerator {

  private final CAsmContext ctx;
  private final StringBuilder builder;

  private final String namespace;
  private final SymbolTable symbolTable;

  private final Stream<AsmGrammarRule> rules;
  private String parserCompareFunction = "equals_insensitive";
  private String currentRuleTypeString = "invalid";

  private final Map<AsmGrammarElement, String> elementVarName = new HashMap<>();
  private final SymbolTable grammarElementSymbolTable = new SymbolTable("ELEM_");
  private final Set<String> functionDefinitions = new HashSet<>();


  /**
   * Constructor.
   */
  public AssemblyParserCodeGenerator(String namespace,
                                     boolean isParserCaseSensitive,
                                     Stream<AsmGrammarRule> rules) {
    this.builder = new StringBuilder();
    this.ctx = new CAsmContext(builder::append, (ctx, element)
        -> AssemblyParserCodeGeneratorDispatcher.dispatch(this, ctx, element));

    this.namespace = namespace;
    if (isParserCaseSensitive) {
      parserCompareFunction = "equals";
    }
    symbolTable = new SymbolTable("VAR_");
    this.rules = rules;
  }

  /**
   * Generates the C++ code for the grammar rules passed in the constructor.
   *
   * @return the generated code
   */
  public String generateRules() {
    rules.forEach(ctx::gen);

    // add functions definitions before grammar rules
    var returnSB = new StringBuilder();
    functionDefinitions.forEach(f -> returnSB.append(f).append("\n"));
    returnSB.append(this.builder);

    return returnSB.toString();
  }

  private String varName(AsmGrammarElement element) {
    return elementVarName.computeIfAbsent(element,
        key -> grammarElementSymbolTable.getNextVariable());
  }

  @Handler
  void handle(CAsmContext ctx, AsmGrammarRule rule) {
    ctx.gen(rule);
    ctx.ln();
  }

  @Handler
  void handle(CAsmContext ctx, AsmNonTerminalRule rule) {
    var type = rule.getAsmType().toCppTypeString(namespace);
    this.currentRuleTypeString = type;
    ctx.spacedIn();
    ctx.ln("RuleParsingResult<%s> %sAsmRecursiveDescentParser::%s() {", type, namespace,
        rule.simpleName());

    ctx.gen(rule.getAlternatives());

    var resultVar = writeCastIfNecessary(ctx, rule.getAlternatives().asmType(), rule.getAsmType(),
        varName(rule.getAlternatives()));
    ctx.ln("return RuleParsingResult<%s>(%s);", type, resultVar);
    ctx.spaceOut();
    ctx.ln("}");
    ctx.spaceOut();
  }

  @Handler
  void handle(CAsmContext ctx, AsmBuiltinRule rule) {
    if (!rule.simpleName().equals("Expression")) {
      throw new ViamError("Unknown AsmParser builtin: " + rule.simpleName());
    }
    var tempVar = symbolTable.getNextVariable();
    var type = rule.getAsmType().toCppTypeString(namespace);

    ctx.spacedIn();
    ctx.ln("RuleParsingResult<%s> %sAsmRecursiveDescentParser::%s() {",
        type, namespace, rule.simpleName());
    ctx.spacedIn();
    ctx.ln("RuleParsingResult<%s> %s = BuiltinExpression();", type, tempVar);
    ctx.ln("if(!%s.Success) {", tempVar);
    ctx.spacedIn();
    ctx.ln("return RuleParsingResult<%s>(%s.getError());", type, tempVar);
    ctx.spaceOut();
    ctx.ln("}");
    ctx.ln("return %s;", tempVar);
    ctx.spaceOut();
    ctx.ln("}");
    ctx.spaceOut();
  }

  @Handler
  void handle(CAsmContext ctx, AsmTerminalRule rule) {
    var dataAction = rule.getAsmType() == ConstantAsmType.instance() ? "tok.getIntVal()" :
        (rule.getAsmType() == StringAsmType.instance() ? "tok.getString()" : "NoData()");

    var token = ParserGenerator.getLlvmTokenKind(rule.simpleName());
    var type = rule.getAsmType().toCppTypeString(namespace);

    //ctx.spacedIn();
    ctx.ln("RuleParsingResult<%s> %sAsmRecursiveDescentParser::%s() {",
        type, namespace, rule.simpleName());
    ctx.spacedIn();
    ctx.ln("auto tok = Lexer.getTok();");
    ctx.ln("if(tok.getKind() != %s) {", token);
    ctx.spacedIn();
    ctx.ln("return RuleParsingResult<%s>(tok.getLoc(),", type);
    ctx.spacedIn();
    ctx.ln("\"Expected %s, but got '\" + tok.getString() + \"'\");", rule.simpleName());
    ctx.spaceOut();
    ctx.spaceOut();
    ctx.ln("} else {");
    ctx.spacedIn();
    ctx.ln("Lexer.Lex();");
    ctx.ln("return RuleParsingResult<%s>(ParsedValue<%s>(%s,tok.getLoc(), tok.getEndLoc()));", type,
        type, dataAction);
    ctx.spaceOut();
    ctx.ln("}");
    ctx.spaceOut();
    ctx.ln("}");
    ctx.spaceOut();
  }

  @Handler
  void handle(CAsmContext ctx, AsmAlternative element) {
    var elementCount =
        element.elements().stream().filter(e -> !(e instanceof AsmLocalVarDefinition)).count();

    if (elementCount > 1 && element.asmType() instanceof GroupAsmType groupAsmType) {
      groupAsmType.getSubtypeMap().forEach(
          (attribute, type) ->
              ctx.ln("ParsedValue<%s> %s;", type.toCppTypeString(namespace), attribute));
    }

    element.elements().forEach(ctx::gen);

    if (elementCount == 1) {
      // if the alternative contains only one element
      // the type of the alternative is just the type of the single element
      ctx.ln("ParsedValue<%s> %s = %s;", element.asmType().toCppTypeString(namespace),
          varName(element), varName(element.elements().get(0)));
    } else if (element.asmType() instanceof GroupAsmType groupType) {
      writeAlternativeGroupStruct(ctx, groupType, varName(element));
    } else {
      throw new ViamError(
          "Alternative with more than one element and AsmType that is not GroupAsmType.");
    }
  }

  @Handler
  void handle(CAsmContext ctx, AsmAlternatives element) {
    var alternativesResultVar = symbolTable.getNextVariable();
    var type = element.asmType().toCppTypeString(namespace);
    var alternatives = element.alternatives();

    if (alternatives.size() == 1) {
      ctx.gen(alternatives.get(0));
      ctx.ln("ParsedValue<%s> %s = %s;",
          type, varName(element), varName(alternatives.get(0)));
      return;
    }

    ctx.ln("std::optional<RuleParsingResult<%s>> %s;",
        type, alternativesResultVar);
    ctx.ln(getAlternativeGuard(alternatives.get(0)));
    ctx.spacedIn();
    ctx.gen(alternatives.get(0));
    ctx.ln("%s = %s;", alternativesResultVar, varName(alternatives.get(0)));
    ctx.spaceOut();
    ctx.ln("}");

    for (int i = 1; i < alternatives.size(); i++) {
      ctx.ln("else %s", getAlternativeGuard(alternatives.get(i)));
      ctx.spacedIn();
      ctx.gen(alternatives.get(i));
      ctx.ln("%s = %s;", alternativesResultVar, varName(alternatives.get(i)));
      ctx.spaceOut();
      ctx.ln("}");
    }

    ctx.ln("else {");
    ctx.spacedIn();
    ctx.ln("return RuleParsingResult<%s>(Lexer.getTok().getLoc(), \"%s\");",
        currentRuleTypeString, alternativesErrorMessage(element));
    ctx.ln("}");
    ctx.spaceOut();

    ctx.ln("ParsedValue<%s> %s = %s.value();",
        type, varName(element), alternativesResultVar);
  }

  @Handler
  void handle(CAsmContext ctx, AsmAssignToAttribute element) {
    ctx.wr(" %s", element.getAssignToName());
  }

  @Handler
  void handle(CAsmContext ctx, AsmAssignToLocalVar element) {
    ctx.wr(" %s", element.getAssignToName());
  }

  @Handler
  void handle(CAsmContext ctx, AsmFunctionInvocation element) {
    var functionGenerator = new PureFunctionCodeGenerator(element.function());
    functionDefinitions.add(functionGenerator.genFunctionDefinition());

    var paramVars = new ArrayList<String>();
    element.parameters().forEach(
        parameter -> {
          ctx.gen(parameter);
          paramVars.add(varName(parameter) + ".Value");
        }
    );

    var functionReturnType = AsmType.getAsmTypeFromOperationalType(element.function().returnType());
    var type = functionReturnType.toCppTypeString(namespace);
    var tempVar = symbolTable.getNextVariable();

    ctx.ln("ParsedValue<%s> %s = ParsedValue<%s>(%s(%s));",
        type, tempVar, type, element.function().simpleName(), String.join(", ", paramVars));

    var resultVar = writeCastIfNecessary(ctx, functionReturnType, element.asmType(), tempVar);
    writeAssignToIfNotNull(ctx,
        element.assignToElement(), tempVar);
    writeToElementVar(ctx, element.asmType(), varName(element), resultVar);
  }

  @Handler
  void handle(CAsmContext ctx, AsmGroup element) {
    ctx.gen(element.alternatives());

    var resultVar = writeCastIfNecessary(ctx, element.alternatives().asmType(), element.asmType(),
        varName(element.alternatives()));
    ctx.ln("ParsedValue<%s> %s = %s;", element.asmType().toCppTypeString(namespace),
        varName(element), resultVar);
  }

  @Handler
  void handle(CAsmContext ctx, AsmLocalVarDefinition element) {
    // TODO: RuleParsingResult or ParsedValue?
    if (element.asmLiteral() != null) {
      ctx.ln("RuleParsingResult<%s> %s;", element.asmType().toCppTypeString(namespace),
          element.localVarName());
      ctx.gen(element.asmLiteral());
      // TODO: cast if necessary
      ctx.ln("%s = %s;", element.localVarName(), varName(element.asmLiteral()));
    } else {
      ctx.ln("RuleParsingResult<NoData> %s;\n", element.localVarName());
    }
  }

  @Handler
  void handle(CAsmContext ctx, AsmLocalVarUse element) {
    // FIXME: is there a cast possible here?
    writeAssignToIfNotNull(ctx,
        element.assignToElement(), element.invokedLocalVar());
    // TODO: writeToElementVar(element.asmType(), ); ?
  }

  @Handler
  void handle(CAsmContext ctx, AsmOption element) {
    var tempVar = symbolTable.getNextVariable();
    var type = element.alternatives().asmType();
    var condition = getGuardCondition(element.semanticPredicate(), element.firstTokens());

    ctx.ln("ParsedValue<%s> %s;", type, tempVar);
    ctx.ln("if (%s) {", condition);
    ctx.spacedIn();
    ctx.gen(element.alternatives());
    ctx.ln("%s = %s;", tempVar, varName(element.alternatives()));
    ctx.spaceOut();
    ctx.ln("}");
    ctx.ln("%s = %s;", varName(element), tempVar);
  }

  @Handler
  void handle(CAsmContext ctx, AsmRepetition element) {
    ctx.ln("while (%s) {", getGuardCondition(element.semanticPredicate(), element.firstTokens()));
    ctx.spacedIn();
    ctx.gen(element.alternatives());
    ctx.spaceOut();
    ctx.ln("}");
  }

  @Handler
  void handle(CAsmContext ctx, AsmRuleInvocation element) {
    var tempVar = symbolTable.getNextVariable();
    var resultType = element.asmType();
    var ruleType = element.rule().getAsmType();

    ctx.ln("RuleParsingResult<%s> %s = %s();", ruleType.toCppTypeString(namespace),
        tempVar, element.rule().simpleName());
    ctx.ln("if(!%s.Success) {", tempVar);
    ctx.spacedIn();
    ctx.ln("return RuleParsingResult<%s>(%s.getError());", currentRuleTypeString, tempVar);
    ctx.spaceOut();
    ctx.ln("}");

    var resultVar = writeCastIfNecessary(ctx, ruleType, resultType, tempVar);
    writeAssignToIfNotNull(ctx, element.assignToElement(), resultVar);
    writeToElementVar(ctx, resultType, varName(element), resultVar);
  }

  @Handler
  void handle(CAsmContext ctx, AsmStringLiteralUse element) {
    if (element.value().trim().isEmpty()) {
      return;
    }

    var tempVar = symbolTable.getNextVariable();
    var type = element.asmType();

    ctx.ln("RuleParsingResult<StringRef> %s = Literal(\"%s\");", tempVar, element.value());
    ctx.ln("if(!%s.Success) {", tempVar);
    ctx.spacedIn();
    ctx.ln("return RuleParsingResult<%s>(%s.getError());", currentRuleTypeString, tempVar);
    ctx.spaceOut();
    ctx.ln("}");

    var resultVar = writeCastIfNecessary(ctx, StringAsmType.instance(), type, tempVar);
    writeAssignToIfNotNull(ctx, element.assignToElement(), resultVar);
    writeToElementVar(ctx, type, varName(element), resultVar);
  }

  private String getAlternativeGuard(AsmAlternative alternative) {
    return "if ("
        + getGuardCondition(alternative.semanticPredicate(), alternative.firstTokens())
        + ") {";
  }

  private String getGuardCondition(@Nullable Function semanticPredicateFunction,
                                   Set<AsmToken> firstTokens) {
    if (semanticPredicateFunction != null) {
      var functionCodeGenerator = new AsmParserFunctionCodeGenerator(semanticPredicateFunction);
      return functionCodeGenerator.genReturnExpression();
    }

    return firstTokens.stream().map(asmtoken ->
        asmtoken.getStringLiteral() != null
            ? String.format("Lexer.getTok().getString().%s(\"%s\")",
            parserCompareFunction,
            asmtoken.getStringLiteral())
            : String.format("Lexer.getTok().getKind() == %s",
            ParserGenerator.getLlvmTokenKind(asmtoken.getRuleName()))
    ).collect(Collectors.joining(" || "));
  }

  private String alternativesErrorMessage(AsmAlternatives alternatives) {
    var expectedTokens = alternatives.alternatives().stream().flatMap(
        alternative -> alternative.firstTokens().stream()
    ).map(
        token -> token.getStringLiteral() != null
            ? "'" + token.getStringLiteral() + "'"
            : token.getRuleName()
    ).collect(Collectors.joining(", "));
    return "No alternative matched. Expected one of {" + expectedTokens + "}.";
  }

  private void writeAssignToIfNotNull(CAsmContext ctx, @Nullable AsmAssignTo assignTo,
                                      String tempVar) {
    if (assignTo != null) {
      ctx.gen(assignTo);
      if (assignTo.getIsWithinRepetition()) {
        ctx.ln(".Value.insert(%s);", tempVar);
      } else {
        ctx.ln(" = %s.getParsed();", tempVar);
      }
    }
  }

  private String writeCastIfNecessary(CAsmContext ctx, AsmType from, AsmType to,
                                      String curValueVar) {
    if (from == to) {
      return curValueVar;
    }

    var tempVar = symbolTable.getNextVariable();
    // TODO: all types of casts (refer to AsmType canBeCastTo methods)

    ctx.ln("// TODO CAST: FROM %s TO %s, stored in %s", from, to, tempVar);
    return tempVar;
  }

  private void writeToElementVar(CAsmContext ctx, AsmType type, String elementVar,
                                 String curValueVar) {
    ctx.ln("ParsedValue<%s> %s = %s;", type.toCppTypeString(namespace), elementVar, curValueVar);
  }

  private void writeAlternativeGroupStruct(CAsmContext ctx, GroupAsmType type, String resultVar) {
    var tempVar = symbolTable.getNextVariable();
    var typeString = type.toCppTypeString(namespace);

    ctx.ln("%s %s = {", typeString, tempVar);

    type.getSubtypeMap().keySet().forEach(
        attribute -> ctx.ln(attribute + ", ")
    );
    ctx.ln("};");

    ctx.ln("ParsedValue<%s> %s = ParsedValue<%s>(%s);",
        typeString, resultVar, typeString, tempVar);
  }
}
