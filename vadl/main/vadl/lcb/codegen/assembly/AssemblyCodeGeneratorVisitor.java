package vadl.lcb.codegen.assembly;

import static vadl.lcb.codegen.assembly.ParserGenerator.mapParserRecord;

import com.google.common.collect.Streams;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.gcb.passes.assembly.visitors.AssemblyVisitor;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.Identifier;
import vadl.viam.ViamError;
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
public class AssemblyCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor implements
    AssemblyVisitor {

  private final String namespace;
  private final SymbolTable symbolTable;
  private final Stack<String> operands = new Stack<>();

  public AssemblyCodeGeneratorVisitor(String namespace, StringWriter writer) {
    super(writer);
    this.namespace = namespace;
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
    for (var arg : node.arguments()) {
      visit(arg);
    }

    if (node.builtIn() == BuiltInTable.MNEMONIC) {
      var oldSymbol = symbolTable.getLastVariable();
      var symbol = symbolTable.getNextVariable();
      var mnem = symbolTable.getNextVariable();
      var binding = symbolTable.getNextVariable();
      var operandIdentifier = namespace + "ParsedOperand";
      writer.write(StringSubstitutor.replace("""
          ParsedValue<${operandIdentifier}> ${symbol}(${operandIdentifier}::CreateToken(
            ${oldSymbol}.Value, ${oldSymbol}.S, ${oldSymbol}.E));
          ${symbol}.Value.setTarget("mnemonic");
          mnemonic ${mnem} = {${symbol}};
          ParsedValue<mnemonic> ${binding} = ParsedValue<mnemonic>(${mnem});
          """, Map.of(
          "operandIdentifier", operandIdentifier,
          "symbol", symbol,
          "oldSymbol", oldSymbol,
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
        ParsedValue<${operandIdentifier}> ${parsedOperand}(${operandIdentifier}::CreateReg(${parsedSymbol}.Value, ${operandIdentifier}::RegisterKind::rk_IntReg, ${parsedSymbol}.S, ${parsedSymbol}.E));
        ${parsedOperand}.Value.setTarget("${field}");
        ${field} ${register} = ${parsedOperand};
        ParsedValue<${operandIdentifier}> ${binding} = ParsedValue<${operandIdentifier}>(${register});
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
        const MCExpr* ${constExpr} = MCConstantExpr::create(${oldSymbol}.Value, Parser.getContext());
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
}
