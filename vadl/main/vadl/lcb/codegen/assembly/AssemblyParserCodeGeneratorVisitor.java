package vadl.lcb.codegen.assembly;

import static vadl.lcb.codegen.assembly.ParserGenerator.mapParserRecord;

import com.google.common.collect.Streams;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.common.AsmParserFunctionCodeGenerator;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.gcb.passes.assembly.visitors.AssemblyVisitor;
import vadl.lcb.template.CommonVarNames;
import vadl.types.BuiltInTable;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.StringAsmType;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.asm.AsmGrammarVisitor;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
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
            // TODO: save value if needed?
            return RuleParsingResult<${type}>(${tempVar});
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
    writer.write(String.format("RuleParsingResult<%s> %sAsmRecursiveDescentParser::%s() {\n",
        rule.getAsmType().toCppTypeString(namespace), namespace, rule.simpleName()));
    rule.getAlternatives().accept(this);
    writer.write("  }\n");
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
    element.elements().forEach(e -> e.accept(this));
  }

  @Override
  public void visit(AsmAlternatives element) {
    var alternatives = element.alternatives();
    if (alternatives.size() == 1) {
      alternatives.get(0).accept(this);
      return;
    }
    writer.write(getAlternativeGuard(alternatives.get(0)));
    alternatives.get(0).accept(this);
    writer.write("} ");
    for (int i = 1; i < alternatives.size(); i++) {
      writer.write("else " + getAlternativeGuard(alternatives.get(i)));
      alternatives.get(i).accept(this);
      writer.write("} ");
    }

    writer.write("else {\n");
    writer.write(String.format("return RuleParsingResult<%s>(Lexer.getTok().getLoc(),\"%s\")\n",
        element.asmType().toCppTypeString(namespace), alternativesErrorMessage(element)));
    writer.write("} \n");
  }

  private String getAlternativeGuard(AsmAlternative alternative) {
    String condition;
    if (alternative.semanticPredicateFunction() != null) {
      var functionCodeGenerator =
          new AsmParserFunctionCodeGenerator(alternative.semanticPredicateFunction());
      condition = functionCodeGenerator.genReturnExpression();
    } else {
      condition = alternative.firstTokens().stream().map(asmtoken ->
          asmtoken.getStringLiteral() != null
              ? String.format("Lexer.getTok().getString().%s(\"%s\")",
              parserCompareFunction,
              asmtoken.getStringLiteral())
              : String.format("Lexer.getTok().getKind() == %s",
              ParserGenerator.getLlvmTokenKind(asmtoken.getRuleName()))
      ).collect(Collectors.joining(" || "));
    }

    return "if (" + condition + ") {\n";
  }

  private String alternativesErrorMessage(AsmAlternatives alternatives) {
    var expectedTokens = alternatives.alternatives().stream().flatMap(
        alternative -> alternative.firstTokens().stream()
    ).map(
        token -> token.getStringLiteral() != null
            ? '"' + token.getStringLiteral() + '"'
            : token.getRuleName()
    ).collect(Collectors.joining(", "));
    return "No alternative matched. Expected one of " + expectedTokens;
  }

  @Override
  public void visit(AsmAssignToAttribute element) {

  }

  @Override
  public void visit(AsmAssignToLocalVar element) {

  }

  @Override
  public void visit(AsmFunctionInvocation element) {

  }

  @Override
  public void visit(AsmGroup element) {

  }

  @Override
  public void visit(AsmLocalVarDefinition element) {

  }

  @Override
  public void visit(AsmLocalVarUse element) {

  }

  @Override
  public void visit(AsmOption element) {

  }

  @Override
  public void visit(AsmRepetition element) {

  }

  @Override
  public void visit(AsmRuleInvocation element) {

  }

  @Override
  public void visit(AsmStringLiteralUse element) {

  }
}
