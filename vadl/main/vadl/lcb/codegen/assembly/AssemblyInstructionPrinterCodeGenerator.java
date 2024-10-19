package vadl.lcb.codegen.assembly;

import java.io.StringWriter;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.passes.relocation.IdentifyFieldUsagePass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.viam.Instruction;
import vadl.viam.graph.control.ReturnNode;

public class AssemblyInstructionPrinterCodeGenerator {
  private final StringWriter writer = new StringWriter();

  public CppFunctionCode generateFunctionBody(
      Instruction instruction,
      TableGenInstruction tableGenInstruction) {
    var visitor =
        new AssemblyInstructionPrinterCodeGeneratorVisitor(writer,
            instruction,
            tableGenInstruction);

    instruction.assembly().function().behavior().getNodes(ReturnNode.class)
        .forEach(visitor::visit);

    return new CppFunctionCode(writer.toString());
  }
}
