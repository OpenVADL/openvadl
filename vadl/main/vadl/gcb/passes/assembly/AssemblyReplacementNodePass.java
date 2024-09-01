package vadl.gcb.passes.assembly;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * The {@link Assembly} has a definition how the user wants an {@link Instruction} emitted.
 * But to create a parser, we need to know each individual {@link Constant}. LLVM has only
 * a limited set of constants which are allowed. This pass replaces those {@link ConstantNode} to
 * {@link AssemblyConstant}.
 * It also changes {@link BuiltInCall} with {@link vadl.types.BuiltInTable#REGISTER}
 * and {@link vadl.types.BuiltInTable#DECIMAL}.
 */
public class AssemblyReplacementNodePass extends Pass {
  public AssemblyReplacementNodePass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("assemblyReplacementConstantPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    replaceConstants(viam);
    replaceRegisters(viam);

    return null;
  }

  private static void replaceConstants(Specification viam) {
    var affectedNodes = viam.isas().flatMap(isa -> isa.ownInstructions().stream())
        .flatMap(instruction -> instruction.assembly().function().behavior()
            .getNodes(ConstantNode.class))
        .filter(constantNode -> constantNode.constant() instanceof Constant.Str)
        .toList();

    for (var node : affectedNodes) {
      node.replaceAndDelete(new AssemblyConstant((Constant.Str) node.constant()));
    }
  }

  private static void replaceRegisters(Specification viam) {
    var affectedNodes = viam.isas().flatMap(isa -> isa.ownInstructions().stream())
        .flatMap(instruction -> instruction.assembly().function().behavior()
            .getNodes(BuiltInCall.class))
        .filter(x -> x.builtIn() == BuiltInTable.REGISTER)
        .toList();

    for (var node : affectedNodes) {
      node.replaceAndDelete(
          new AssemblyRegisterNode((FieldRefNode) node.arguments().get(0), node.type()));
    }
  }
}
