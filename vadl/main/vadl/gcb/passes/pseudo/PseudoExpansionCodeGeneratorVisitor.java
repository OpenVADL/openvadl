package vadl.gcb.passes.pseudo;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import com.google.common.collect.Streams;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.utils.Pair;
import vadl.viam.Assembly;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Relocation;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
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

  /**
   * The order of the parameters is not necessarily the order in which the expansion should happen.
   * This function looks at the {@link Format} and reorders the list to the same
   * order.
   */
  private List<Pair<Format.Field, ExpressionNode>> reorderParameters(
      Format format,
      List<Pair<Format.Field, ExpressionNode>> pairs) {
    var result = new ArrayList<Pair<Format.Field, ExpressionNode>>();
    var lookup = pairs.stream().collect(Collectors.toMap(Pair::left, Pair::right));
    var usages = fieldUsages.get(format).keySet();
    // The `fieldsSortedByLsbDesc` returns all fields from the format.
    // However, we are only interested in the registers and immediates.
    // That's why we filter with `contains`. `fieldUsages` only stores REGISTER and IMMEDIATE.
    var order = format.fieldsSortedByLsbDesc().filter(usages::contains).toList();

    for (var item : order) {
      var l = ensureNonNull(lookup.get(item),
          () -> Diagnostic.error("Cannot find format's field in pseudo instruction",
              item.sourceLocation()).build());
      result.add(Pair.of(item, l));
    }

    return result;
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

    var reorderedPairs = reorderParameters(instrCallNode.target().format(), pairs);

    for (int index = 0; index < reorderedPairs.size(); index++) {
      var field = reorderedPairs.get(index).left();
      var argument = reorderedPairs.get(index).right();

      if (argument instanceof ConstantNode cn) {
        lowerExpression(instrCallNode.target(), sym, field, cn);
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

  private void lowerExpression(Instruction machineInstruction, String sym, Format.Field field,
                               ConstantNode argument) {
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
        // We know that `field` is used as a register index.
        // But we don't know which register file.
        // We look for the `field` in the machine instruction's behavior and return the usages.
        var registerFiles = machineInstruction.behavior().getNodes(FieldRefNode.class)
            .filter(x -> x.formatField() == field)
            .flatMap(
                fieldRefNode -> fieldRefNode.usages().filter(y -> y instanceof HasRegisterFile))
            .map(x -> ((HasRegisterFile) x).registerFile())
            .distinct()
            .toList();

        ensure(registerFiles.size() == 1,
            () -> Diagnostic.error("Found multiple or none register files for this field.",
                    field.sourceLocation())
                .note(
                    "The pseudo instruction expansion requires one register file to detect "
                        + "the register file name. In this particular case is the field used by "
                        + "multiple register files or none and we don't know which name to use.")
                .build());

        var registerFile =
            ensurePresent(registerFiles.stream().findFirst(), "Expected one register file");

        writer.write(String.format("%s.addOperand(MCOperand::createReg(%s::%s%s));\n",
            sym,
            namespace,
            registerFile.identifier.simpleName(),
            argument.constant().asVal().intValue()));
      }
      default -> throw Diagnostic.error("Cannot generate cpp code for this argument",
          field.sourceLocation()).build();
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
