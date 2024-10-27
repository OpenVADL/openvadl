package vadl.lcb.codegen.expansion;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.pseudo.PseudoExpansionCodeGeneratorVisitor;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.viam.Format;
import vadl.viam.PseudoInstruction;
import vadl.viam.ViamError;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Generates functions which expands {@link PseudoInstruction} in LLVM.
 */
public class PseudoExpansionCodeGenerator extends LcbGenericCodeGenerator {
  private final String namespace;
  private final IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages;
  private final Map<Format.Field, CppFunction> immediateDecodings;
  private final Map<Format.Field, List<VariantKind>> variants;
  private final List<CompilerRelocation> relocations;
  private final PseudoInstruction pseudoInstruction;

  /**
   * Constructor.
   */
  public PseudoExpansionCodeGenerator(String namespace,
                                      IdentifyFieldUsagePass.ImmediateDetectionContainer
                                          fieldUsages,
                                      Map<Format.Field, CppFunction> immediateDecodings,
                                      Map<Format.Field, List<VariantKind>> variants,
                                      List<CompilerRelocation> relocations,
                                      PseudoInstruction pseudoInstruction) {
    this.namespace = namespace;
    this.fieldUsages = fieldUsages;
    this.immediateDecodings = immediateDecodings;
    this.variants = variants;
    this.relocations = relocations;
    this.pseudoInstruction = pseudoInstruction;
  }

  @Override
  protected String generateFunctionBody(CppFunction function) {
    var writer = new StringWriter();
    var instrCallNodes = function.behavior().getNodes(InstrCallNode.class).toList();

    if (instrCallNodes.isEmpty()) {
      throw new ViamError("For the function is an InstrCallNode required.");
    }

    writer.write("std::vector< MCInst > result;\n");
    var visitor =
        new PseudoExpansionCodeGeneratorVisitor(writer, namespace, fieldUsages,
            immediateDecodings, variants, relocations, pseudoInstruction);
    instrCallNodes.forEach(visitor::visit);
    writer.write("return result");
    return writer.toString();
  }
}
