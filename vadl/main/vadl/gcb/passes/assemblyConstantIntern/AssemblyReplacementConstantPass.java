package vadl.gcb.passes.assemblyConstantIntern;

import java.io.IOException;
import java.util.HashMap;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.SymbolTable;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * The {@link Assembly} has a definition how the user wants an {@link Instruction} emitted.
 * But to create a parser, we need to know each individual {@link Constant}. LLVM has only
 * a limited set of constants which are allowed. This pass replaces those {@link ConstantNode} to
 * {@link AssemblyConstant}.
 */
public class AssemblyReplacementConstantPass extends Pass {
  public AssemblyReplacementConstantPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("assemblyReplacementConstantPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var symbolTable = new SymbolTable();
    var affectedNodes = viam.isas().flatMap(isa -> isa.instructions().stream())
        .flatMap(instruction -> instruction.assembly().function().behavior()
            .getNodes(ConstantNode.class))
        .filter(constantNode -> constantNode.constant() instanceof Constant.Str)
        .toList();

    for (var node : affectedNodes) {
      node.replaceAndDelete(new AssemblyConstant((Constant.Str) node.constant()));
    }

    return null;
  }
}
