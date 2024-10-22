package vadl.lcb.codegen.assembly;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.stream.Collectors;
import vadl.cppCodeGen.SymbolTable;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.types.BuiltInTable;
import vadl.utils.SourceLocation;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Visitor for printing instructions for the assembler.
 */
public class AssemblyInstructionPrinterCodeGeneratorVisitor
    implements GraphNodeVisitor {
  private final Instruction instruction;
  private final SymbolTable symbolTable = new SymbolTable();
  private final StringWriter writer;
  private final Deque<String> operands = new ArrayDeque<>();
  private final TableGenInstruction tableGenInstruction;

  /**
   * Constructor.
   */
  public AssemblyInstructionPrinterCodeGeneratorVisitor(
      StringWriter writer,
      Instruction instruction,
      TableGenInstruction tableGenInstruction) {
    this.writer = writer;
    this.instruction = instruction;
    this.tableGenInstruction = tableGenInstruction;
  }

  @Override
  public void visit(ConstantNode node) {
    String symbol = symbolTable.getNextVariable();
    operands.add(symbol);

    if (node.constant() instanceof Constant.Str str) {
      writer.write("std::string " + symbol + " = std::string(\"" + str.value() + "\");\n");
    } else {
      throw Diagnostic.error("Not supported constant type", node.sourceLocation()).build();
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    if (node.builtIn() == BuiltInTable.MNEMONIC) {
      String symbol = symbolTable.getNextVariable();
      operands.add(symbol);

      writer.write(String.format("std::string %s = std::string(\"%s\");\n", symbol,
          instruction.identifier.simpleName()));
    } else if (node.builtIn() == BuiltInTable.CONCATENATE_STRINGS) {
      for (var arg : node.arguments()) {
        visit(arg);
      }

      String symbol = symbolTable.getNextVariable();
      writer.write("std::string " + symbol + " = ");
      for (int i = 0; i < node.arguments().size(); i++) {
        writer.write(operands.removeFirst());
        if (i < node.arguments().size() - 1) {
          writer.write(" + ");
        }
      }
      writer.write(";\n");
      operands.add(symbol);
    } else if (node.builtIn() == BuiltInTable.REGISTER) {
      ensure(node.arguments().size() == 1, "Expected only one argument");
      ensure(node.arguments().get(0) instanceof FieldRefNode,
          "Register argument is not a FieldRefNode");
      var cast = (FieldRefNode) node.arguments().get(0);
      var index = indexInInputs(cast.formatField())
          .or(() -> indexInOutputs(cast.formatField()))
          .orElseThrow(() -> Diagnostic.error(
              "Field is not part of an input or output operand in tablegen",
              cast.sourceLocation()).build());
      var symbol = symbolTable.getNextVariable();

      // We need this helper function "getRegisterName..." because
      // we might need to support multiple register files.
      var registerFileName = getRegisterFile(instruction.behavior(), cast);
      writer.write(
          String.format(
              "std::string %s = "
                  + "getRegisterNameFrom%sByIndex("
                  + "MCOperandWrapper(MI->getOperand(%d)).unwrapToIntegral());" + " // "
                  + cast.formatField().identifier.simpleName() + "\n",
              symbol, registerFileName, index));
      operands.add(symbol);
    } else if (node.builtIn() == BuiltInTable.DECIMAL) {
      ensure(node.arguments().size() == 1, "Expected only one argument");
      writeImmediateWithRadix(node, 10);
    } else if (node.builtIn() == BuiltInTable.HEX) {
      ensure(node.arguments().size() == 1, "Expected only one argument");
      writeImmediateWithRadix(node, 16);
    } else {
      throw Diagnostic.error("Not supported builtin for assembly printing",
              node.sourceLocation())
          .build();
    }
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {

  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {

  }

  @Override
  public void visit(WriteMemNode writeMemNode) {

  }

  @Override
  public void visit(SliceNode sliceNode) {

  }

  @Override
  public void visit(SelectNode selectNode) {

  }

  @Override
  public void visit(ReadRegNode readRegNode) {

  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {

  }

  @Override
  public void visit(ReadMemNode readMemNode) {

  }

  @Override
  public void visit(LetNode letNode) {

  }

  @Override
  public void visit(FuncParamNode funcParamNode) {

  }

  @Override
  public void visit(FuncCallNode funcCallNode) {

  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {

  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {

  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {

  }

  @Override
  public void visit(InstrEndNode instrEndNode) {

  }

  @Override
  public void visit(ReturnNode returnNode) {
    visit(returnNode.value());
    writer.write("return " + operands.pop() + ";\n");
  }

  @Override
  public void visit(BranchEndNode branchEndNode) {

  }

  @Override
  public void visit(InstrCallNode instrCallNode) {

  }

  @Override
  public void visit(IfNode ifNode) {

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
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {

  }

  private void writeImmediateWithRadix(BuiltInCall node, int radix) {
    if (node.arguments().get(0) instanceof FieldRefNode fieldRefNode) {
      writeImmediateWithRadix(fieldRefNode.formatField(), radix, fieldRefNode.sourceLocation());
    } else if (node.arguments().get(0) instanceof FieldAccessRefNode fieldAccessRefNode) {
      writeImmediateWithRadix(fieldAccessRefNode.fieldAccess().fieldRef(), radix,
          fieldAccessRefNode.sourceLocation());
    } else {
      throw Diagnostic.error("Not supported argument "
              + "in assembly printing", node.sourceLocation())
          .build();
    }
  }

  private void writeImmediateWithRadix(Format.Field field, int radix,
                                       SourceLocation sourceLocation) {
    var index = ensurePresent(indexInInputs(field), () ->
        Diagnostic.error("Immediate must be part of an tablegen input.",
            sourceLocation)
    );
    var symbol = symbolTable.getNextVariable();
    writer.write(String.format(
        "std::string %s = AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%d)), %d, &MAI);\n",
        symbol,
        index,
        radix
    ));
    operands.add(symbol);
  }

  private Optional<Integer> indexInInputs(Format.Field needle) {
    int outputOffset = tableGenInstruction.getOutOperands().size();
    for (int i = 0; i < tableGenInstruction.getInOperands().size(); i++) {
      var operand = tableGenInstruction.getInOperands().get(i);
      if (operand instanceof ReferencesFormatField x
          && x.formatField().equals(needle)) {
        return Optional.of(outputOffset + i);
      }
    }

    return Optional.empty();
  }

  private Optional<Integer> indexInOutputs(Format.Field needle) {
    for (int i = 0; i < tableGenInstruction.getOutOperands().size(); i++) {
      var operand = tableGenInstruction.getOutOperands().get(i);
      if (operand instanceof ReferencesFormatField x
          && x.formatField().equals(needle)) {
        return Optional.of(i);
      }
    }

    return Optional.empty();
  }


  private String getRegisterFile(Graph behavior, FieldRefNode fieldRefNode) {
    var candidates = behavior.getNodes(FieldRefNode.class)
        .filter(x -> x.formatField().equals(fieldRefNode.formatField()))
        .flatMap(Node::usages)
        .filter(x -> x instanceof HasRegisterFile)
        .map(x -> (HasRegisterFile) x)
        .collect(Collectors.toSet());

    if (candidates.isEmpty()) {
      throw Diagnostic.error(
          "Field is not used as register. Therefore the compiler generator cannot "
              +
              "detect the register file.",
          fieldRefNode.sourceLocation()).build();
    } else if (candidates.size() > 1) {
      throw Diagnostic.error(
          "Field is by multiple register file. Therefore the compiler generator cannot "
              +
              "detect the register file for the assembly.",
          fieldRefNode.sourceLocation()).build();
    } else {
      return candidates.iterator().next().registerFile().identifier.simpleName();
    }
  }
}