package vadl.lcb.codegen.assembly;

import static vadl.lcb.codegen.assembly.ParserGenerator.mapParserRecord;

import com.google.common.collect.Streams;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.common.AsmParserFunctionCodeGenerator;
import vadl.cppCodeGen.common.PureFunctionCodeGenerator;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.gcb.passes.assembly.visitors.AssemblyVisitor;
import vadl.lcb.template.CommonVarNames;
import vadl.types.BuiltInTable;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.asm.AsmGrammarVisitor;
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
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Generates the cpp code for assembly parsing.
 */
@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
public class AssemblyParserCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor implements
    AssemblyVisitor, AsmGrammarVisitor {

  private final String namespace;
  private final SymbolTable symbolTable;
  @Nullable
  private final Instruction instruction;
  private final ArrayDeque<String> operands = new ArrayDeque<>();

  private String parserCompareFunction = "equals_insensitive";
  private String currentRuleTypeString = "invalid";

  private final Map<AsmGrammarElement, String> elementVarName = new HashMap<>();
  private final SymbolTable grammarElementSymbolTable = new SymbolTable("ELEM_");
  private final Set<String> functionDefinitions = new HashSet<>();

  /**
   * Constructor.
   */
  public AssemblyParserCodeGeneratorVisitor(String namespace, Instruction instruction,
                                            StringWriter writer) {
    super(writer);
    this.namespace = namespace;
    this.instruction = instruction;
    symbolTable = new SymbolTable("VAR_");
  }

  /**
   * Constructor.
   */
  public AssemblyParserCodeGeneratorVisitor(String namespace,
                                            boolean isParserCaseSensitive,
                                            StringWriter writer) {
    super(writer);
    this.namespace = namespace;
    this.instruction = null;
    if (isParserCaseSensitive) {
      parserCompareFunction = "equals";
    }
    symbolTable = new SymbolTable("VAR_");
  }

  private String varName(AsmGrammarElement element) {
    return elementVarName.computeIfAbsent(element,
        key -> grammarElementSymbolTable.getNextVariable());
  }

  @Override
  public void visit(ZeroExtendNode node) {

  }

  @Override
  public void visit(SignExtendNode node) {

  }

  @Override
  public void visit(TruncateNode node) {

  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {

  }

  @Override
  public void visit(BuiltInCall node) {
    if (instruction == null) {
      return;
    }
    for (var arg : node.arguments()) {
      visit(arg);
    }

    if (node.builtIn() == BuiltInTable.MNEMONIC) {
      var oldSymbol = symbolTable.getNextVariable();
      var oldSymbolStrLit = symbolTable.getNextVariable();
      var symbol = symbolTable.getNextVariable();
      var mnem = symbolTable.getNextVariable();
      var binding = symbolTable.getNextVariable();
      var operandIdentifier = namespace + "ParsedOperand";
      var lit = instruction.identifier.simpleName();
      writer.write(StringSubstitutor.replace("""
          RuleParsingResult<StringRef> ${oldSymbol} = Literal("${lit}");
          if(!${oldSymbol}.Success) {
              return RuleParsingResult<NoData>(${oldSymbol}.getError());
          }
          ParsedValue<StringRef> ${oldSymbolStrLit} = ${oldSymbol}.getParsed();
          ParsedValue<${operandIdentifier}> ${symbol}(${operandIdentifier}::CreateToken(
            ${oldSymbol}.Value, ${oldSymbol}.S, ${oldSymbol}.E));
          ${symbol}.Value.setTarget("mnemonic");
          mnemonic ${mnem} = {${symbol}};
          ParsedValue<mnemonic> ${binding} = ParsedValue<mnemonic>(${mnem});
          """, Map.of(
          "operandIdentifier", operandIdentifier,
          "symbol", symbol,
          "oldSymbol", oldSymbol,
          "lit", lit,
          "oldSymbolStrLit", oldSymbolStrLit,
          "mnem", mnem,
          "binding", binding
      )));
      operands.add(mnem);
    } else if (node.builtIn() == BuiltInTable.REGISTER) {
      for (var arg : node.arguments()) {
        visit(arg);
      }
    } else if (node.builtIn() == BuiltInTable.DECIMAL
        || node.builtIn() == BuiltInTable.HEX) {
      for (var arg : node.arguments()) {
        visit(arg);
      }
    } else if (node.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
      var symbol = symbolTable.getNextVariable();
      var sequence = symbolTable.getNextVariable();
      var structName = mapParserRecord(node).structName();

      var ops = new ArrayList<String>();
      for (int i = 0; i < node.arguments().size(); i++) {
        ops.add(operands.pop());
      }
      Collections.reverse(ops);

      var fields = Streams.zip(node.arguments().stream()
                  .map(ParserGenerator::mapToName).filter(x -> !x.isEmpty()), ops.stream(),
              (field, variable) -> variable + ".Value." + field)
          .collect(Collectors.joining(",\n"));

      var pushBacks = node.arguments().stream()
          .map(ParserGenerator::mapToName)
          .filter(x -> !x.isEmpty())
          .map(field -> String.format("""
              Operands.push_back(std::make_unique<%sParsedOperand>(%s.Value.%s.Value));
              """, namespace, sequence, field))
          .collect(Collectors.joining("\n"));

      var result = StringSubstitutor.replace("""
          ${structName} ${symbol} = {
            ${fields}
          };
          ParsedValue<${structName}> ${sequence} = ParsedValue<${structName}>(${symbol});
          ${pushBacks}
          """, Map.of("structName", structName,
          "fields", fields,
          "sequence", sequence,
          "symbol", symbol,
          "pushBacks", pushBacks));

      writer.write(result);
    } else {
      throw new ViamError("not supported");
    }
  }

  @Override
  public void visit(AssemblyConstant node) {
    var symbol = symbolTable.getNextVariable();
    var unwrappedSymbol = symbolTable.getNextVariable();
    writer.write(StringSubstitutor.replace("""
        RuleParsingResult<StringRef> ${symbol} = Literal("${lit}");
        if(!${symbol}.Success) {
          return RuleParsingResult<NoData>(${symbol}.getError());
        }
        ParsedValue<StringRef> ${unwrappedSymbol} = ${symbol}.getParsed();
        """, Map.of(
        "symbol", symbol,
        "lit", ((Constant.Str) node.constant()).value(),
        "unwrappedSymbol", unwrappedSymbol
    )));
    operands.add(unwrappedSymbol);
  }

  @Override
  public void visit(FieldRefNode node) {
    var symbol = symbolTable.getNextVariable();
    var parsedSymbol = symbolTable.getNextVariable();
    var parsedOperand = symbolTable.getNextVariable();
    var register = symbolTable.getNextVariable();
    var binding = symbolTable.getNextVariable();
    var operandIdentifier = namespace + "ParsedOperand";
    var field = node.formatField().identifier.simpleName();

    writer.write(StringSubstitutor.replace("""
        RuleParsingResult<uint64_t /* UInt<64> */> ${symbol} = Register();
        if(!${symbol}.Success) {
            return RuleParsingResult<NoData>(${symbol}.getError());
        }
        ParsedValue<uint64_t /* UInt<64> */> ${parsedSymbol} = ${symbol}.getParsed();
        ParsedValue<${operandIdentifier}>
          ${parsedOperand}(${operandIdentifier}::CreateReg(${parsedSymbol}.Value,
            ${operandIdentifier}::RegisterKind::rk_IntReg, ${parsedSymbol}.S, ${parsedSymbol}.E));
        ${parsedOperand}.Value.setTarget("${field}");
        ${field} ${register} = ${parsedOperand};
        ParsedValue<${operandIdentifier}> ${binding} =
          ParsedValue<${operandIdentifier}>(${register});
        """, Map.of(
        "symbol", symbol,
        "parsedSymbol", parsedSymbol,
        "parsedOperand", parsedOperand,
        "register", register,
        "binding", binding,
        "operandIdentifier", operandIdentifier,
        "field", field
    )));

    operands.add(binding);
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {
    var operandIdentifier = namespace + "ParsedOperand";
    var oldSymbol = symbolTable.getLastVariable();
    var constExpr = symbolTable.getNextVariable();
    var symbol = symbolTable.getNextVariable();
    var parsed = symbolTable.getNextVariable();
    var imm = symbolTable.getNextVariable();
    var binding = symbolTable.getNextVariable();

    writer.write(StringSubstitutor.replace("""
        const MCExpr* ${constExpr} = MCConstantExpr::create(${oldSymbol}.Value,
          Parser.getContext());
        ParsedValue<${operandIdentifier}> ${symbol}(${operandIdentifier}::CreateImm(
          ${constExpr}, ${oldSymbol}.S, {oldSymbol}.E));
        ${symbol}.Value.setTarget("${lit}");
        ${lit} ${imm} = {${symbol}};
        ParsedValue<${lit}> ${binding} = ParsedValue<${lit}>(${imm});
        """, Map.of(
        "operandIdentifier", operandIdentifier,
        "constExpr", constExpr,
        "symbol", symbol,
        "oldSymbol", oldSymbol,
        "parsed", parsed,
        "imm", imm,
        "binding", binding,
        "lit", fieldAccessRefNode.fieldAccess().identifier.simpleName()
    )));
  }

  @Override
  public void visit(ReturnNode returnNode) {
    visit(returnNode.value());
    writer.write("""
        return ParsedValue<NoData>(NoData());
        """);
  }

  @Override
  public void visit(AsmGrammarRule rule) {
    rule.accept(this);
    functionDefinitions.forEach(writer::write);
  }

  @Override
  public void visit(AsmBuiltinRule rule) {
    if (!rule.simpleName().equals("Expression")) {
      throw new ViamError("Unknown AsmParser builtin: " + rule.simpleName());
    }
    var tempVar = symbolTable.getNextVariable();

    writer.write(StringSubstitutor.replace("""
          RuleParsingResult<${type}> ${namespace}AsmRecursiveDescentParser::${ruleName}() {
            RuleParsingResult<${type}> ${tempVar} = BuiltinExpression();
            if(!${tempVar}.Success) {
                return RuleParsingResult<${type}>(${tempVar}.getError());
            }
        
            return ${tempVar};
          }
        
        """, Map.of(
        "type", rule.getAsmType().toCppTypeString(namespace),
        CommonVarNames.NAMESPACE, namespace,
        "ruleName", rule.simpleName(),
        "tempVar", tempVar
    )));
  }

  @Override
  public void visit(AsmNonTerminalRule rule) {
    var type = rule.getAsmType().toCppTypeString(namespace);
    this.currentRuleTypeString = type;

    writer.write(String.format("  RuleParsingResult<%s> %sAsmRecursiveDescentParser::%s() {\n",
        type, namespace, rule.simpleName()));
    rule.getAlternatives().accept(this);

    var resultVar = writeCastIfNecessary(rule.getAlternatives().asmType(), rule.getAsmType(),
        varName(rule.getAlternatives()));
    writer.write(String.format("    return RuleParsingResult<%s>(%s);\n  }\n\n", type, resultVar));
  }

  @Override
  public void visit(AsmTerminalRule rule) {

    var dataAction = rule.getAsmType() == ConstantAsmType.instance() ? "tok.getIntVal()" :
        (rule.getAsmType() == StringAsmType.instance() ? "tok.getString()" : "NoData()");

    var token = ParserGenerator.getLlvmTokenKind(rule.simpleName());

    writer.write(StringSubstitutor.replace("""
          RuleParsingResult<${type}> ${namespace}AsmRecursiveDescentParser::${ruleName}() {
            auto tok = Lexer.getTok();
            if(tok.getKind() != ${token}) {
              return RuleParsingResult<${type}>(tok.getLoc(),
                "Expected ${ruleName}, but got '" + tok.getString() + "'");
            } else {
              Lexer.Lex();
              return RuleParsingResult<${type}>(ParsedValue<${type}>(${dataAction},
                tok.getLoc(), tok.getEndLoc()));
            }
          }
        
        """, Map.of(
        "type", rule.getAsmType().toCppTypeString(namespace),
        CommonVarNames.NAMESPACE, namespace,
        "ruleName", rule.simpleName(),
        "token", token,
        "dataAction", dataAction
    )));
  }

  @Override
  public void visit(AsmAlternative element) {

    var elementCount =
        element.elements().stream().filter(e -> !(e instanceof AsmLocalVarDefinition)).count();

    if (elementCount > 1 && element.asmType() instanceof GroupAsmType groupAsmType) {
      groupAsmType.getSubtypeMap().forEach((attribute, type) -> writer.write(
          String.format("  ParsedValue<%s> %s;\n", type.toCppTypeString(namespace), attribute)));
    }

    element.elements().forEach(e -> e.accept(this));

    if (elementCount == 1) {
      // if the alternative contains only one element
      // the type of the alternative is just the type of the single element
      writer.write(
          String.format("ParsedValue<%s> %s = %s;\n", element.asmType().toCppTypeString(namespace),
              varName(element), varName(element.elements().get(0))));
    } else if (element.asmType() instanceof GroupAsmType groupType) {
      writeAlternativeGroupStruct(groupType, varName(element));
    } else {
      throw new ViamError(
          "Alternative with more than one element and AsmType that is not GroupAsmType.");
    }
  }

  @Override
  public void visit(AsmAlternatives element) {
    var alternativesResultVar = symbolTable.getNextVariable();
    var type = element.asmType().toCppTypeString(namespace);
    var alternatives = element.alternatives();

    if (alternatives.size() == 1) {
      alternatives.get(0).accept(this);
      writer.write(String.format("ParsedValue<%s> %s = %s;\n",
          type, varName(element), varName(alternatives.get(0))));
      return;
    }

    writer.write(
        String.format("std::optional<RuleParsingResult<%s>> %s;\n",
            type, alternativesResultVar));
    writer.write(getAlternativeGuard(alternatives.get(0)));
    alternatives.get(0).accept(this);
    writer.write(String.format("%s = %s;\n", alternativesResultVar, varName(alternatives.get(0))));
    writer.write("} ");

    for (int i = 1; i < alternatives.size(); i++) {
      writer.write("else " + getAlternativeGuard(alternatives.get(i)));
      alternatives.get(i).accept(this);
      writer.write(
          String.format("%s = %s;\n", alternativesResultVar, varName(alternatives.get(i))));
      writer.write("} ");
    }

    writer.write(StringSubstitutor.replace("""
        else {
          return RuleParsingResult<${ruleType}>(Lexer.getTok().getLoc(), "${error}");
        }
        
        ParsedValue<${type}> ${elementVar} = ${alternativeResult}.value();
        """, Map.of(
        "ruleType", currentRuleTypeString,
        "error", alternativesErrorMessage(element),
        "elementVar", varName(element),
        "alternativeResult", alternativesResultVar
    )));
  }

  private String getAlternativeGuard(AsmAlternative alternative) {
    return "if ("
        + getGuardCondition(alternative.semanticPredicate(), alternative.firstTokens())
        + ") {\n";
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

  @Override
  public void visit(AsmAssignToAttribute element) {
    writer.write(String.format(" %s", element.getAssignToName()));
  }

  @Override
  public void visit(AsmAssignToLocalVar element) {
    writer.write(String.format(" %s", element.getAssignToName()));
  }

  @Override
  public void visit(AsmFunctionInvocation element) {
    var functionGenerator = new PureFunctionCodeGenerator(element.function());
    functionDefinitions.add(functionGenerator.genFunctionDefinition());

    var paramVars = new ArrayList<String>();
    element.parameters().forEach(
        parameter -> {
          parameter.accept(this);
          paramVars.add(varName(parameter) + ".Value");
        }
    );

    var functionReturnType = AsmType.getAsmTypeFromOperationalType(element.function().returnType());
    var tempVar = symbolTable.getNextVariable();

    writer.write(StringSubstitutor.replace("""
        ParsedValue<${type}> ${tempVar} = ParsedValue<${type}>(${functionName}(${params}));
        """, Map.of(
        "type", functionReturnType.toCppTypeString(namespace),
        "tempVar", tempVar,
        "functionName", element.function().simpleName(),
        "params", String.join(", ", paramVars)
    )));

    var resultVar = writeCastIfNecessary(functionReturnType, element.asmType(), tempVar);
    writeAssignToIfNotNull(
        element.assignToElement(), tempVar);
    writeToElementVar(element.asmType(), varName(element), resultVar);
  }

  @Override
  public void visit(AsmGroup element) {
    element.alternatives().accept(this);

    var resultVar = writeCastIfNecessary(element.alternatives().asmType(), element.asmType(),
        varName(element.alternatives()));
    writer.write(String.format("ParsedValue<%s> %s = %s;\n",
        element.asmType().toCppTypeString(namespace), varName(element), resultVar));
  }

  @Override
  public void visit(AsmLocalVarDefinition element) {


    if (element.asmLiteral() != null) {
      writer.write(
          String.format("RuleParsingResult<%s> %s;\n", element.asmType().toCppTypeString(namespace),
              element.localVarName()));
      element.asmLiteral().accept(this);
      // TODO: cast if necessary
      writer.write(
          String.format("%s = %s;\n", element.localVarName(), varName(element.asmLiteral())));
    } else {
      writer.write(String.format("RuleParsingResult<NoData> %s;\n", element.localVarName()));
    }
  }

  @Override
  public void visit(AsmLocalVarUse element) {
    // FIXME: is there a cast possible here?
    writeAssignToIfNotNull(
        element.assignToElement(), element.invokedLocalVar());
    // TODO: writeToElementVar(element.asmType(), ); ?
  }

  @Override
  public void visit(AsmOption element) {
    var tempVar = symbolTable.getNextVariable();
    var type = element.alternatives().asmType();

    writer.write(StringSubstitutor.replace("""
          ParsedValue<${type}> ${tempVar};
          if (${condition}) {
        """, Map.of(
        "type", type.toCppTypeString(namespace),
        "tempVar", tempVar,
        "condition", getGuardCondition(element.semanticPredicate(), element.firstTokens())
    )));
    element.alternatives().accept(this);
    writer.write(StringSubstitutor.replace("""
          ${tempVar} = ${altResultVar};
          }
          ${optionElemVar} = ${tempVar};
        """, Map.of(
        "tempVar", tempVar,
        "altResultVar", varName(element.alternatives()),
        "optionElemVar", varName(element)
    )));
  }

  @Override
  public void visit(AsmRepetition element) {
    writer.write("while (" + getGuardCondition(element.semanticPredicate(), element.firstTokens())
        + ") {\n");
    element.alternatives().accept(this);
    writer.write("}\n");
  }

  @Override
  public void visit(AsmRuleInvocation element) {

    var tempVar = symbolTable.getNextVariable();
    var resultType = element.asmType();
    var ruleType = element.rule().getAsmType();

    writer.write(StringSubstitutor.replace("""
          RuleParsingResult<${ruleType}> ${tempVar} = ${ruleName}();
          if(!${tempVar}.Success) {
              return RuleParsingResult<${currentRuleType}>(${tempVar}.getError());
          }
        """, Map.of(
        "tempVar", tempVar,
        "ruleName", element.rule().simpleName(),
        "ruleType", ruleType.toCppTypeString(namespace),
        "currentRuleType", currentRuleTypeString,
        "elementVar", varName(element)
    )));

    var resultVar = writeCastIfNecessary(ruleType, resultType, tempVar);
    writeAssignToIfNotNull(element.assignToElement(), resultVar);
    writeToElementVar(resultType, varName(element), resultVar);
  }

  @Override
  public void visit(AsmStringLiteralUse element) {
    if (element.value().trim().isEmpty()) {
      return;
    }

    var tempVar = symbolTable.getNextVariable();
    var type = element.asmType();

    writer.write(StringSubstitutor.replace("""
          RuleParsingResult<StringRef> ${tempVar} = Literal("${stringLiteral}");
          if(!${tempVar}.Success) {
              return RuleParsingResult<${ruleType}>(${tempVar}.getError());
          }
        """, Map.of(
        "tempVar", tempVar,
        "stringLiteral", element.value(),
        "ruleType", currentRuleTypeString,
        "elementVar", varName(element)
    )));

    var resultVar = writeCastIfNecessary(StringAsmType.instance(), type, tempVar);
    writeAssignToIfNotNull(element.assignToElement(), resultVar);
    writeToElementVar(type, varName(element), resultVar);
  }

  private void writeAssignToIfNotNull(@Nullable AsmAssignTo assignTo,
                                      String tempVar) {
    if (assignTo != null) {
      assignTo.accept(this);
      if (assignTo.getIsWithinRepetition()) {
        writer.write(String.format(".Value.insert(%s);\n", tempVar));
      } else {
        writer.write(String.format(" = %s.getParsed();\n", tempVar));
      }
    }
  }

  private String writeCastIfNecessary(AsmType from, AsmType to, String curValueVar) {
    if (from == to) {
      return curValueVar;
    }

    var tempVar = symbolTable.getNextVariable();
    // TODO: all types of casts (refer to AsmType canBeCastTo methods)

    writer.write(String.format("  //TODO CAST: FROM %s TO %s, stored in %s\n", from, to, tempVar));
    return tempVar;
  }

  private void writeToElementVar(AsmType type, String elementVar, String curValueVar) {
    writer.write(
        String.format("ParsedValue<%s> %s = %s;\n", type.toCppTypeString(namespace), elementVar,
            curValueVar));
  }

  private void writeAlternativeGroupStruct(GroupAsmType type, String resultVar) {
    var tempVar = symbolTable.getNextVariable();
    var typeString = type.toCppTypeString(namespace);

    writer.write(typeString + " " + tempVar + " = {\n");
    type.getSubtypeMap().keySet().forEach(
        attribute -> writer.write(attribute + ",\n")
    );
    writer.write("};\n");

    writer.write(String.format("ParsedValue<%s> %s = ParsedValue<%s>(%s);\n",
        typeString, resultVar, typeString, tempVar));
  }
}
