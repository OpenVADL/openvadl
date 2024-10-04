package vadl.gcb.passes.pseudo;

import static vadl.viam.ViamError.ensure;

import com.google.common.collect.Streams;
import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Relocation;
import vadl.viam.ViamError;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * More specialised generator visitor for generating the expansion of pseudo instructions.
 */
public class PseudoExpansionCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor {
  private final SymbolTable symbolTable = new SymbolTable();
  private final String namespace;
  private final DetectImmediatePass.ImmediateDetectionContainer fieldUsages;
  private final Map<Format.Field, CppFunction> immediateDecodings;
  private final IdentityHashMap<Format.Field, VariantKind> immVariants;
  private final List<ElfRelocation> relocations;

  /**
   * Constructor.
   */
  public PseudoExpansionCodeGeneratorVisitor(StringWriter writer, String namespace,
                                             DetectImmediatePass.ImmediateDetectionContainer
                                                 fieldUsages,
                                             Map<Format.Field, CppFunction> immediateDecodings,
                                             IdentityHashMap<Format.Field, VariantKind> immVariants,
                                             List<ElfRelocation> relocations) {
    super(writer);
    this.namespace = namespace;
    this.fieldUsages = fieldUsages;
    this.immediateDecodings = immediateDecodings;
    this.immVariants = immVariants;
    this.relocations = relocations;
  }

  @Override
  public void visit(InstrCallNode instrCallNode) {
    var sym = symbolTable.getNextVariable();
    writer.write(String.format("MCInst %s = MCInst();\n", sym));
    writer.write(String.format("%s.setOpcode(%s::%s);\n", sym, namespace,
        instrCallNode.target().identifier.simpleName()));

    var pairs =
        Streams.zip(instrCallNode.getParamFields().stream(), instrCallNode.arguments().stream(),
            Pair::of).toList();

    for (int index = 0; index < pairs.size(); index++) {
      var field = pairs.get(index).left();
      var argument = pairs.get(index).right();

      if (argument instanceof ConstantNode cn) {
        lowerExpression(sym, field, cn);
      } else if (argument instanceof FuncCallNode fn) {
        ensure(fn.function() instanceof Relocation,
            () -> Diagnostic.error("Function must be a relocation", fn.sourceLocation()).build());
        lowerExpressionWithRelocation(sym, field, index, (Relocation) fn.function());
      } else if (argument instanceof FuncParamNode) {
        lowerExpressionWithImmOrRegister(sym, field, index);
      } else {
        throw Diagnostic.error("Not implemented for this node type.", argument.sourceLocation())
            .build();
      }
    }

    writer.write(String.format("result.push_back(%s);\n", sym));
  }

  @Override
  public void visit(ConstantNode node) {

  }


  @Override
  public void visit(FuncParamNode node) {

  }


  @Override
  public void visit(InstrEndNode instrEndNode) {
    instrEndNode.sideEffects().forEach(this::visit);
  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {
    throw new RuntimeException("not implemented");
  }

  private void lowerExpression(String sym, Format.Field field, ConstantNode argument) {
    var usage = fieldUsages.get(field.format()).get(field);
    ensure(usage != null, "usage must not be null");
    switch (usage) {
      case IMMEDIATE -> {
        var decodingFunction = immediateDecodings.get(field);
        ensure(decodingFunction != null, "decodingFunction must not be null");
        var decodingFunctionName = decodingFunction.functionName().lower();
        writer.write(
            String.format("%s.addOperand(MCOperand::createImm(%s(%s)));\n", sym,
                decodingFunctionName,
                argument.constant().asVal().intValue()));
      }
      case REGISTER -> {
        /*
        TODO this doesn't work because we do not know which register file
        writer.write(String.format("%s.addOperand(MCOperand::createReg(%s::%s));\n",
            sym,
            namespace,
            argument.constant().asVal().intValue()));
         */
      }
      default -> throw new ViamError("not supported");
    }
  }

  private void lowerExpressionWithRelocation(String sym,
                                             Format.Field field,
                                             int argumentIndex,
                                             Relocation relocation) {
    // Here we generate the relocations.
    var argumentSymbol = symbolTable.getNextVariable();
    writer.write(
        String.format("const MCExpr* %s = MCOperandToMCExpr(instruction.getOperand(%d));\n",
            argumentSymbol,
            argumentIndex));

    var argumentRelocationSymbol = symbolTable.getNextVariable();
    var logicalRelocation =
        relocations.stream().filter(x -> x.logicalRelocation().relocation() == relocation)
            .findFirst();
    ensure(logicalRelocation.isPresent(), "logicalRelocation must exist");
    var variant = logicalRelocation.get().logicalRelocation().variantKind().value();
    writer.write(
        String.format("MCOperand %s = "
                + "MCOperand::createExpr(%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, Ctx));\n",
            argumentRelocationSymbol, namespace, argumentSymbol, namespace, variant));
    writer.write(String.format("%s.addOperand(%s);\n",
        sym,
        argumentRelocationSymbol));
  }

  private void lowerExpressionWithImmOrRegister(String sym,
                                                Format.Field field,
                                                int argumentIndex) {
    var usage = fieldUsages.get(field.format()).get(field);
    ensure(usage != null, "usage must not be null");
    switch (usage) {
      case IMMEDIATE -> {
        var argumentSymbol = symbolTable.getNextVariable();
        writer.write(
            String.format("const MCExpr* %s = MCOperandToMCExpr(instruction.getOperand(%d));\n",
                argumentSymbol,
                argumentIndex));

        var argumentImmSymbol = symbolTable.getNextVariable();
        var variant = immVariants.get(field);
        ensure(variant != null, "variant must exist: %s", field.identifier.lower());
        writer.write(
            String.format(
                "MCOperand %s = "
                    +
                    "MCOperand::createExpr(%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, "
                    + "Ctx));\n",
                argumentImmSymbol, namespace, argumentSymbol, namespace, variant.value()));
        writer.write(String.format("%s.addOperand(%s);\n",
            sym,
            argumentImmSymbol));
      }
      case REGISTER -> writer.write(
          String.format("%s.addOperand(instruction.getOperand(%d));\n", sym, argumentIndex));
      default -> throw new ViamError("not supported");
    }
  }
}
