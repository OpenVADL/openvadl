package vadl.lcb.codegen.assembly;

import java.io.StringWriter;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.gcb.passes.assemblyConstantIntern.AssemblyConstant;
import vadl.gcb.passes.assemblyConstantIntern.visitors.AssemblyVisitor;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
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
    symbolTable = new SymbolTable();
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
      writer.write(String.format("""
              ParsedValue<%s> %s(%s::CreateToken(%s.Value, %s.S, %s.E));
              %s.Value.setTarget("mnemonic");
              mnemonic %s = {%s};
              ParsedValue<mnemonic> %s = ParsedValue<mnemonic>(%s);
              """,
          operandIdentifier,
          symbol,
          operandIdentifier,
          oldSymbol,
          oldSymbol,
          oldSymbol,
          symbol,
          mnem,
          symbol,
          binding,
          mnem));
    }
  }

  @Override
  public void visit(AssemblyConstant node) {
    var symbol = symbolTable.getNextVariable();
    var unwrappedSymbol = symbolTable.getNextVariable();
    writer.write(String.format("""
            RuleParsingResult<StringRef> %s = Literal("%s");
            if(!%s.Success) {
                return RuleParsingResult<NoData>(%s.getError());
            }
            ParsedValue<StringRef> %s = %s.getParsed();
            """, symbol,
        ((Constant.Str) node.constant()).value(),
        symbol,
        symbol,
        unwrappedSymbol,
        symbol));
  }

  @Override
  public void visit(FieldRefNode node) {
    var symbol = symbolTable.getNextVariable();
    var parsedSymbol = symbolTable.getNextVariable();
    var parsedOperand = symbolTable.getNextVariable();
    var register = symbolTable.getNextVariable();
    var binding = symbolTable.getNextVariable();
    var operandIdentifier = namespace + "ParsedOperand";
    writer.write(String.format("""
            RuleParsingResult<uint64_t /* UInt<64> */> %s = Register();
            if(!%s.Success) {
                return RuleParsingResult<NoData>(%s.getError());
            }
            ParsedValue<uint64_t /* UInt<64> */> %s = %s.getParsed();
            ParsedValue<%s> %s(%s::CreateReg(%s.Value, %s::RegisterKind::rk_IntReg, %s.S, %s.E));
            %s.Value.setTarget("%s");
            %s %s = {%s};
            ParsedValue<%s> %s = ParsedValue<%s>(%s);
            """,
        symbol,
        symbol,
        symbol,
        parsedSymbol,
        symbol,
        operandIdentifier,
        parsedOperand,
        operandIdentifier,
        parsedSymbol,
        operandIdentifier,
        parsedSymbol,
        parsedOperand,
        parsedOperand,
        node.formatField().identifier.simpleName(),
        node.formatField().identifier.simpleName(),
        register,
        parsedOperand,
        node.formatField().identifier.simpleName(),
        binding,
        node.formatField().identifier.simpleName(),
        register));
  }
}
