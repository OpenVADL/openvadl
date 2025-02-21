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
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.ExpressionAsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.ModifierAsmType;
import vadl.types.asmTypes.OperandAsmType;
import vadl.types.asmTypes.RegisterAsmType;
import vadl.types.asmTypes.StatementsAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.types.asmTypes.SymbolAsmType;
import vadl.types.asmTypes.VoidAsmType;
import vadl.utils.SourceLocation;
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
  }

  @Handler
  void handle(CAsmContext ctx, AsmNonTerminalRule rule) {
    if (rule.simpleName().equals("Instruction")
        && rule.getAlternatives().alternatives().isEmpty()) {
      handleEmptyInstructionRule(ctx);
      return;
    }

    var type = rule.getAsmType().toCppTypeString(namespace);
    this.currentRuleTypeString = type;
    ctx.ln("RuleParsingResult<%s> %sAsmRecursiveDescentParser::%s() {", type, namespace,
        rule.simpleName());
    ctx.spacedIn();

    ctx.gen(rule.getAlternatives());

    var resultVar = writeCastIfNecessary(ctx, rule.getAlternatives().asmType(), rule.getAsmType(),
        varName(rule.getAlternatives()), false);
    ctx.ln("return RuleParsingResult<%s>(%s);", type, resultVar);
    ctx.spaceOut();
    ctx.ln("}");
    ctx.spaceOut();
    ctx.ln();
  }

  private void handleEmptyInstructionRule(CAsmContext ctx) {
    ctx.ln("RuleParsingResult<NoData> %sAsmRecursiveDescentParser::Instruction() {", namespace);
    ctx.spacedIn();
    ctx.ln("return RuleParsingResult<NoData>(ParsedValue<NoData>(NoData {}));");
    ctx.spaceOut();
    ctx.ln("}");
    ctx.ln();

    DeferredDiagnosticStore.add(Diagnostic.warning(
        "There are no instructions defined in the assembly description grammar."
            + "The generated assembler will not be able to parse any instructions.",
        SourceLocation.INVALID_SOURCE_LOCATION));
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
    ctx.ln();
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
    ctx.ln();
  }

  @Handler
  void handle(CAsmContext ctx, AsmAlternative element) {
    var elementCount =
        element.elements().stream().filter(e -> !(e instanceof AsmLocalVarDefinition)).count();

    if (elementCount > 1 && element.asmType() instanceof GroupAsmType groupAsmType) {
      groupAsmType.getSubtypeMap().forEach(
          (attribute, type) ->
              ctx.ln("std::optional<ParsedValue<%s>> %s;", type.toCppTypeString(namespace),
                  attribute));
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

    ctx.ln("std::optional<ParsedValue<%s>> %s;",
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
    // Handled in writeAssignToIfNotNull
  }

  @Handler
  void handle(CAsmContext ctx, AsmAssignToLocalVar element) {
    // Handled in writeAssignToIfNotNull
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

    var resultVar =
        writeCastIfNecessary(ctx, functionReturnType, element.asmType(), tempVar, false);
    writeAssignToIfNotNull(ctx, element.assignToElement(), tempVar);
    writeToElementVar(ctx, element.asmType(), varName(element), resultVar);
  }

  @Handler
  void handle(CAsmContext ctx, AsmGroup element) {
    var tempVar = symbolTable.getNextVariable();
    var type = element.asmType().toCppTypeString(namespace);
    ctx.ln("std::optional<ParsedValue<%s>> %s;", type, tempVar);
    ctx.ln("{");
    ctx.spacedIn();

    ctx.gen(element.alternatives());

    var resultVar = writeCastIfNecessary(ctx, element.alternatives().asmType(), element.asmType(),
        varName(element.alternatives()), false);

    if (element.isEnclosingAlternativeOfAsmGroupType()) {
      writeAssignToIfNotNull(ctx, element.assignTo(), resultVar);
    }

    writeToElementVar(ctx, element.asmType(), varName(element), resultVar);
    ctx.ln("%s = %s;", tempVar, resultVar);
    ctx.spaceOut();
    ctx.ln("}");

    ctx.ln("ParsedValue<%s> %s = %s.value();", type, varName(element), tempVar);
    ctx.ln();
  }

  @Handler
  void handle(CAsmContext ctx, AsmLocalVarDefinition element) {
    ctx.ln("ParsedValue<%s> %s;", element.asmType().toCppTypeString(namespace),
        element.localVarName());

    if (element.asmLiteral() != null) {
      ctx.gen(element.asmLiteral());
    }
  }

  @Handler
  void handle(CAsmContext ctx, AsmLocalVarUse element) {
    var resultVar = writeCastIfNecessary(ctx, element.invokedLocalVarType(), element.asmType(),
        element.invokedLocalVar(), false);
    writeAssignToIfNotNull(ctx, element.assignToElement(), resultVar);
    writeToElementVar(ctx, element.asmType(), varName(element), resultVar);
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

    var resultVar = writeCastIfNecessary(ctx, ruleType, resultType, tempVar, true);
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

    var resultVar = writeCastIfNecessary(ctx, StringAsmType.instance(), type, tempVar, true);
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
      if (assignTo.isPlusEqualsAssignment()) {
        ctx.ln("%s.Value.push_back(%s.Value);", assignTo.getAssignToName(), tempVar);
      } else {
        ctx.ln("%s = %s;", assignTo.getAssignToName(), tempVar);
      }
    }
  }

  private void writeToElementVar(CAsmContext ctx, AsmType type, String elementVar,
                                 String curValueVar) {
    ctx.ln("ParsedValue<%s> %s = %s;", type.toCppTypeString(namespace), elementVar, curValueVar);
  }

  private void writeAlternativeGroupStruct(CAsmContext ctx, GroupAsmType type, String resultVar) {
    var tempVar = symbolTable.getNextVariable();
    var typeString = type.toCppTypeString(namespace);

    ctx.ln("%s %s = {", typeString, tempVar);
    ctx.spacedIn();
    type.getSubtypeMap().keySet().forEach(
        attribute -> ctx.ln(attribute + ".value(), ")
    );
    ctx.spaceOut();
    ctx.ln("};");

    ctx.ln("ParsedValue<%s> %s = ParsedValue<%s>(%s);",
        typeString, resultVar, typeString, tempVar);
  }

  /**
   * Writes a cast from one AsmType to another.
   * A cast is necessary if {@code from} and {@code to} parameters are not the same.
   * In the case they are the same, the method just returns the {@code curValueVar}.
   *
   * @param ctx         the context to write the generated code to
   * @param from        the AsmType to cast from
   * @param to          the AsmType to cast to
   * @param curValueVar the variable holding the value to be cast
   * @return the variable holding the cast value
   */
  private String writeCastIfNecessary(CAsmContext ctx, AsmType from, AsmType to,
                                      String curValueVar, boolean isRuleParsingResult) {


    var paredValueVar = symbolTable.getNextVariable();
    if (isRuleParsingResult) {
      ctx.ln("ParsedValue<%s> %s = %s.getParsed();", from.toCppTypeString(namespace), paredValueVar,
          curValueVar);
      curValueVar = paredValueVar;
    }

    if (from == to) {
      return isRuleParsingResult ? paredValueVar : curValueVar;
    }

    var tempVar = symbolTable.getNextVariable();
    var destination = "ParsedValue<" + to.toCppTypeString(namespace) + "> " + tempVar;
    var loc = curValueVar + ".S, " + curValueVar + ".E";

    if (from instanceof GroupAsmType fromGroupType) {

      var subTypeMap = fromGroupType.getSubtypeMap();
      var keys = subTypeMap.keySet().stream().toList();
      var subTypes = subTypeMap.values().stream().toList();

      // (@operand, @operand, ...) to @instruction
      // push operands to the operand vector to be used in AsmParser::MatchAndEmitInstruction()
      if (to == InstructionAsmType.instance()
          && subTypes.stream().allMatch(val -> val == OperandAsmType.instance())) {
        ctx.ln("Operands.push_back(std::make_unique<%sParsedOperand>(%s.Value.mnemonic.Value));",
            namespace, curValueVar);
        String finalCurValueVar = curValueVar;
        keys.forEach(
            attribute -> {
              if (!attribute.equals("mnemonic")) {
                ctx.ln("Operands.push_back(std::make_unique<%sParsedOperand>(%s.Value.%s.Value));",
                    namespace, finalCurValueVar, attribute);
              }
            }
        );
        ctx.ln("%s = ParsedValue<NoData>(NoData());", destination);
        return tempVar;
      }

      // (@modifier @expression) to @operand
      if (to == OperandAsmType.instance() && subTypeMap.size() == 2
          && subTypes.get(0) == ModifierAsmType.instance()
          && subTypes.get(1) == ExpressionAsmType.instance()) {
        var modifier = curValueVar + ".Value." + keys.get(0) + ".Value";
        var expr = curValueVar + ".Value." + keys.get(1) + ".Value";
        var modifiedExpr = symbolTable.getNextVariable();
        ctx.ln(
            "const MCExpr* %s = %sMCExpr::create(%s, %s, Parser.getContext());",
            modifiedExpr, namespace, expr, modifier);
        ctx.ln(
            "SMLoc S = %s.S.getPointer() < %s.S.getPointer() ? %s.S : %s.S;",
            modifier, expr, modifier, expr);
        ctx.ln(
            "SMLoc E = %s.E.getPointer() < %s.E.getPointer() ? %s.E : %s.E;",
            modifier, expr, modifier, expr);
        ctx.ln("%s(%sParsedOperand::CreateImm(%s, S, E))", destination, namespace, modifiedExpr);
        return tempVar;
      }

      // (@instruction, @instruction, ...) to @statements
      if (to == StatementsAsmType.instance()
          && subTypes.stream().allMatch(subtype -> subtype == InstructionAsmType.instance())) {
        ctx.wr("ParsedValue<std::vector<NoData>> %s (std::vector<NoData>{", tempVar);
        String finalCurValueVar = curValueVar;
        keys.forEach(key -> ctx.wr("%s.Value.%s.Value", finalCurValueVar, key));
        ctx.ln("});");
        return tempVar;
      }

      // (@operand, @operand, ...) to @operands
      if (to == OperandAsmType.instance()
          && subTypes.stream().allMatch(val -> val == OperandAsmType.instance())) {
        ctx.wr("ParsedValue<std::vector<%sParsedOperand>> %s (std::vector<%sParsedOperand>{",
            namespace, tempVar, namespace);
        String finalCurValueVar = curValueVar;
        keys.forEach(key -> ctx.wr("%s.Value.%s.Value", finalCurValueVar, key));
        ctx.ln("};");
        return tempVar;
      }

      // (@type) to @type
      if (subTypes.size() == 1 && subTypes.get(0) == to) {
        ctx.ln("%s = %s.Value.%s;", destination, curValueVar, keys.get(0));
        return tempVar;
      }
    }

    if (from == ConstantAsmType.instance()) {
      // to @operand
      if (to == OperandAsmType.instance()) {
        var expr = symbolTable.getNextVariable();
        ctx.ln("const MCExpr* %s = MCConstantExpr::create(%s.Value, Parser.getContext());", expr,
            curValueVar);
        ctx.ln("%s(%sParsedOperand::CreateImm(%s, %s));", destination, namespace, expr, loc);
        return tempVar;
      }

      // to @register
      if (to == RegisterAsmType.instance()) {
        ctx.ln("%s(%s.Value, %s);", destination, curValueVar, loc);
        return tempVar;
      }
    }

    if (from == StringAsmType.instance()) {
      // to @operand
      if (to == OperandAsmType.instance()) {
        ctx.ln("%s(%sParsedOperand::CreateToken(%s.Value,%s));", destination, namespace,
            curValueVar, loc);
        return tempVar;
      }

      // to @symbol
      if (to == SymbolAsmType.instance()) {
        ctx.ln("%s = %s;", destination, curValueVar);
        return tempVar;
      }

      // to @register
      if (to == RegisterAsmType.instance()) {
        var regNoVar = symbolTable.getNextVariable();
        ctx.ln("unsigned %s;", regNoVar);
        ctx.ln("if(!AsmUtils::MatchRegNo(%s.Value, %s)) {", curValueVar, regNoVar);
        ctx.spacedIn();
        ctx.ln("return RuleParsingResult<%s>(%s.S,\"Could not convert data into register"
                + "because '\" + %s.Value + \"' is not a valid register\");",
            currentRuleTypeString, curValueVar, curValueVar);
        ctx.spaceOut();
        ctx.ln("}");
        ctx.ln("%s(%s, %s);", destination, regNoVar, loc);
        return tempVar;
      }

      // to @modifier
      if (to == ModifierAsmType.instance()) {
        var modifier = symbolTable.getNextVariable();
        ctx.ln("%sMCExpr::VariantKind %s;", namespace, modifier);
        ctx.ln("if(!AsmUtils::MatchCustomModifier(%s.Value, %s)) {", curValueVar, modifier);
        ctx.spacedIn();
        ctx.ln("return RuleParsingResult<%s>(%s.S,\"Could not convert data into modifier because"
                + " '\" +  %s.Value + \"' is not a valid modifier\");",
            currentRuleTypeString, curValueVar, curValueVar);
        ctx.spaceOut();
        ctx.ln("}");
        ctx.ln("%s(%s, %s);", destination, modifier, loc);
        return tempVar;
      }
    }

    if (from == RegisterAsmType.instance()) {
      // to @operand
      if (to == OperandAsmType.instance()) {
        ctx.ln("%s(%sParsedOperand::CreateReg("
                + "%s.Value, %sParsedOperand::RegisterKind::rk_IntReg, %s));",
            destination, namespace, curValueVar, namespace, loc);
        return tempVar;
      }
    }

    if (from == ExpressionAsmType.instance()) {
      // to @operand
      if (to == OperandAsmType.instance()) {
        ctx.ln("%s(%sParsedOperand::CreateImm(%s.Value,%s));", destination, namespace, curValueVar,
            loc);
        return tempVar;
      }
    }

    if (from == SymbolAsmType.instance()) {
      // to @operand
      if (to == OperandAsmType.instance()) {
        var symbol = symbolTable.getNextVariable();
        var expr = symbolTable.getNextVariable();
        ctx.ln("const MCSymbol* %s = Parser.getContext().getOrCreateSymbol(%s.Value);", symbol,
            curValueVar);
        ctx.ln("const MCExpr* %s = MCSymbolRefExpr::create(%s, Parser.getContext());", expr,
            symbol);
        ctx.ln("%s(%sParsedOperand::CreateImm(%s,%s));", destination, namespace, expr, loc);
        return tempVar;
      }
    }

    if (from == OperandAsmType.instance()) {
      // to @operands
      if (to == OperandAsmType.instance()) {
        ctx.ln("ParsedValue<std::vector<%sParsedOperand>> %s"
                + "(std::vector<%sParsedOperand>{ %s.Value });",
            namespace, tempVar, namespace, curValueVar);
        return tempVar;
      }
    }

    if (from == InstructionAsmType.instance()) {
      // to @statements
      if (to == StatementsAsmType.instance()) {
        ctx.ln("ParsedValue<std::vector<NoData>> %s "
            + "(std::vector<NoData>{ %s.Value });", tempVar, curValueVar);
        return tempVar;
      }
    }

    if (to == VoidAsmType.instance()) {
      ctx.ln("ParsedValue<NoData> %s = ParsedValue<NoData>(NoData());", tempVar);
      return tempVar;
    }

    throw new ViamError("Unknown AsmType cast in asm parser from " + from + " to " + to);
  }
}
