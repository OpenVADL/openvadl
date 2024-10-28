package vadl.gcb.passes.pseudo;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import com.google.common.collect.Streams;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.cppCodeGen.GenericCppCodeGeneratorVisitor;
import vadl.cppCodeGen.SymbolTable;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.error.Diagnostic;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Relocation;
import vadl.viam.ViamError;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * More specialised generator visitor for generating the expansion of pseudo instructions.
 */
public class PseudoExpansionCodeGeneratorVisitor extends GenericCppCodeGeneratorVisitor {
  private final SymbolTable symbolTable = new SymbolTable();
  private final String namespace;
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final Map<Format.Field, CppFunction> immediateDecodings;
  private final Map<Format.Field, List<VariantKind>> immVariants;
  private final List<CompilerRelocation> relocations;
  private final PseudoInstruction pseudoInstruction;

  /**
   * Constructor.
   */
  public PseudoExpansionCodeGeneratorVisitor(StringWriter writer, String namespace,
                                             IdentifyFieldUsagePass.ImmediateDetectionContainer
                                                 fieldUsages,
                                             Map<Format.Field, CppFunction> immediateDecodings,
                                             Map<Format.Field, List<VariantKind>> immVariants,
                                             List<CompilerRelocation> relocations,
                                             PseudoInstruction pseudoInstruction) {
    super(writer);
    this.namespace = namespace;
    this.fieldUsages = fieldUsages;
    this.immediateDecodings = immediateDecodings;
    this.immVariants = immVariants;
    this.relocations = relocations;
    this.pseudoInstruction = pseudoInstruction;
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
    var usages = fieldUsages.getFieldUsages(format).keySet();
    // The `fieldsSortedByLsbDesc` returns all fields from the format.
    // However, we are only interested in the registers and immediates.
    // That's why we filter with `contains`. `fieldUsages` only stores REGISTER and IMMEDIATE.
    var order = format.fieldsSortedByLsbDesc().filter(usages::contains).toList();

    for (var item : order) {
      var l = ensureNonNull(lookup.get(item),
          () -> Diagnostic.error("Cannot find format's field in pseudo instruction",
              item.sourceLocation()));
      result.add(Pair.of(item, l));
    }

    return result;
  }


  /**
   * Trying to get the index from {@code pseudoInstruction.parameters} based on the given
   * {@code parameter}. This index is important because it is the index of the pseudo instruction's
   * operands which will be used for the pseudo instruction's expansion.
   * For example, you have pseudo instruction {@code X(arg1, arg2) } which has two arguments. You
   * have to know that {@code arg2} has index {@code 1} to correctly map it to a machine
   * instruction later.
   */
  private int getOperandIndexFromPseudoInstruction(Format.Field field,
                                                   ExpressionNode argument,
                                                   Identifier parameter) {
    for (int i = 0; i < pseudoInstruction.parameters().length; i++) {
      if (parameter.simpleName()
          .equals(pseudoInstruction.parameters()[i].identifier.simpleName())) {
        return i;
      }
    }

    throw Diagnostic.error(
            "Cannot assign field because the field was not field.",
            parameter.sourceLocation())
        .locationDescription(argument.sourceLocation(), "Trying to match this argument.")
        .locationDescription(field.sourceLocation(), "Trying to assign this field.")
        .locationDescription(pseudoInstruction.sourceLocation(),
            "This pseudo instruction is affected.")
        .help(
            String.format("The parameter '%s' must match any pseudo instruction's parameter names",
                parameter.simpleName()))
        .build();
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

    reorderedPairs.forEach(pair -> {
      var field = pair.left();
      var argument = pair.right();

      if (argument instanceof ConstantNode cn) {
        lowerExpression(instrCallNode.target(), sym, field, cn);
      } else if (argument instanceof FuncCallNode fn) {
        ensure(fn.function() instanceof Relocation,
            () -> Diagnostic.error("Function must be a relocation", fn.sourceLocation()));        /*
         pseudo instruction CALL( symbol : Bits<32> ) =
         {
              LUI{ rd = 1 as Bits5, imm = hi20( symbol ) }
              JALR{ rd = 1 as Bits5, rs1 = 1 as Bits5, imm = lo12( symbol ) }
         }
         Here we want to match the `symbol` from `CALL` with `hi20`'s `symbol`.
         */
        var relocationArgument =
            ensurePresent(Arrays.stream(fn.function().parameters()).findFirst(),
                () -> Diagnostic.error("Function does not have a parameter in pseudo instruction",
                    fn.sourceLocation()));
        var pseudoInstructionIndex =
            getOperandIndexFromPseudoInstruction(field, argument, relocationArgument.identifier);
        lowerExpressionWithRelocation(sym, field, pseudoInstructionIndex,
            (Relocation) fn.function());
      } else if (argument instanceof FuncParamNode funcParamNode) {
        var pseudoInstructionIndex =
            getOperandIndexFromPseudoInstruction(field, argument,
                funcParamNode.parameter().identifier);
        lowerExpressionWithImmOrRegister(sym, field, pseudoInstructionIndex);
      } else if (argument instanceof ZeroExtendNode zeroExtendNode
          && zeroExtendNode.value() instanceof ConstantNode cn) {
        lowerExpression(instrCallNode.target(), sym, field, cn);
      } else {
        throw Diagnostic.error("Not implemented for this node type.", argument.sourceLocation())
            .build();
      }
    });

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

  private void lowerExpression(Instruction machineInstruction,
                               String sym, Format.Field field,
                               ConstantNode argument) {
    var usage = fieldUsages.getFieldUsages(field.format()).get(field);
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
            .filter(x -> x.formatField().equals(field))
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
        );

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
                                             @SuppressWarnings("unused")
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
    var elfRelocation =
        relocations.stream().filter(x -> x.relocation() == relocation)
            .findFirst();
    ensure(elfRelocation.isPresent(), "elfRelocation must exist");
    var variant = elfRelocation.get().variantKind().value();
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
    var usage = fieldUsages.getFieldUsages(field.format()).get(field);
    ensure(usage != null, "usage must not be null");
    switch (usage) {
      case IMMEDIATE -> {
        var argumentSymbol = symbolTable.getNextVariable();
        writer.write(
            String.format("const MCExpr* %s = MCOperandToMCExpr(instruction.getOperand(%d));\n",
                argumentSymbol,
                argumentIndex));

        var argumentImmSymbol = symbolTable.getNextVariable();
        var variants = immVariants.get(field);
        ensure(variants != null, () -> Diagnostic.error(
                String.format("Variant must exist for the field '%s' but it doesn't.",
                    field.identifier.lower()),
                field.sourceLocation())
            .note(
                "The compiler generator tries to lower an immediate in the "
                    + "pseudo expansion. To do so it requires to generate variants for immediates. "
                    + "It seems like that that this variants was not generated.")
        );
        var variant = Objects.requireNonNull(variants).get(0);
        writer.write(
            String.format(
                "MCOperand %s = "
                    +
                    "MCOperand::createExpr(%sMCExpr::create(%s, %sMCExpr::VariantKind::%s, "
                    + "Ctx));\n",
                argumentImmSymbol, namespace, argumentSymbol, namespace,
                Objects.requireNonNull(variant.value())));
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
