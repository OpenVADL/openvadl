package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.lcb.codegen.assembly.AssemblyCodeGeneratorVisitor;
import vadl.lcb.codegen.assembly.ParserGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;

/**
 * This file includes the definitions for the asm parser.
 */
public class EmitAsmRecursiveDescentParserCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmRecursiveDescentParserCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().processorName().value()
        + "/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  /**
   * The parser has methods for lexes which are used.
   */
  record RuleParsingResultForLex(AssemblyConstant.TOKEN_KIND kind, String functionName) {

  }

  /**
   * Every instruction has a method in the parser which parses the assembly.
   */
  record RuleParsingResultWhenInstruction(String functionName, String body) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var lexes = lexes(specification);
    var instructions = instructions(specification);
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "lexParsingResults", lexes,
        "instructionResults", instructions);
  }

  @NotNull
  private static List<RuleParsingResultForLex> lexes(Specification specification) {
    return specification.isas().flatMap(isa -> isa.instructions().stream())
        .flatMap(instruction -> instruction.assembly().function().behavior().getNodes(
            AssemblyConstant.class))
        .sorted(Comparator.comparing(AssemblyConstant::kind)) // Sort by something
        .map(assemblyConstant -> new RuleParsingResultForLex(
            assemblyConstant.kind(),
            ParserGenerator.generateConstantName(assemblyConstant)))
        .distinct()
        .toList();
  }

  @NotNull
  private List<RuleParsingResultWhenInstruction> instructions(Specification specification) {
    return specification.isas()
        .flatMap(isa -> isa.instructions().stream())
        .map(instruction -> {
          var writer = new StringWriter();
          var visitor =
              new AssemblyCodeGeneratorVisitor(lcbConfiguration().processorName().value(),
                  instruction,
                  writer);
          var returnNode =
              GraphUtils.getSingleNode(instruction.assembly().function().behavior(),
                  ReturnNode.class);
          visitor.visit(returnNode);

          return new RuleParsingResultWhenInstruction(
              ParserGenerator.generateInstructionName(instruction), writer.toString());
        })
        .toList();
  }
}
