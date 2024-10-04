package vadl.lcb.codegen.expansion;

import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.pseudo.PseudoExpansionCodeGeneratorVisitor;
import vadl.gcb.passes.relocation.DetectImmediatePass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.codegen.LcbCodeGenerator;
import vadl.viam.Format;
import vadl.viam.PseudoInstruction;
import vadl.viam.ViamError;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.ReturnNode;

/**
 * Generates functions which expands {@link PseudoInstruction} in LLVM.
 */
public class PseudoExpansionCodeGenerator extends LcbCodeGenerator {
  private final String namespace;
  private final DetectImmediatePass.ImmediateDetectionContainer fieldUsages;
  private final Map<Format.Field, CppFunction> immediateDecodings;
  private final IdentityHashMap<Format.Field, VariantKind> variants;
  private final List<ElfRelocation> relocations;

  /**
   * Constructor.
   */
  public PseudoExpansionCodeGenerator(String namespace,
                                      DetectImmediatePass.ImmediateDetectionContainer fieldUsages,
                                      Map<Format.Field, CppFunction> immediateDecodings,
                                      IdentityHashMap<Format.Field, VariantKind> variants,
                                      List<ElfRelocation> relocations) {
    this.namespace = namespace;
    this.fieldUsages = fieldUsages;
    this.immediateDecodings = immediateDecodings;
    this.variants = variants;
    this.relocations = relocations;
  }

  @Override
  protected String generateFunctionBody(CppFunction function) {
    var writer = new StringWriter();
    var endNodes = Stream.concat(function.behavior().getNodes(InstrCallNode.class),
        function.behavior().getNodes(ReturnNode.class)
    ).toList();

    if (endNodes.isEmpty()) {
      throw new ViamError("For the function is a return node required.");
    }

    writer.write("std::vector< MCInst > result;\n");
    var visitor =
        new PseudoExpansionCodeGeneratorVisitor(writer, namespace, fieldUsages,
            immediateDecodings, variants, relocations);
    endNodes.forEach(visitor::visit);
    writer.write("return result");
    return writer.toString();
  }
}
