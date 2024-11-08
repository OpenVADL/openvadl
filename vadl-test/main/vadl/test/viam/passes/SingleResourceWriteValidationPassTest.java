package vadl.test.viam.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.utils.ViamUtils.findDefinitionsByFilter;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.visualize.DotGraphVisualizer;
import vadl.viam.passes.SingleResourceWriteValidationPass;

public class SingleResourceWriteValidationPassTest extends AbstractTest {

  @Test
  void testSingleResourceWriteValidationPass() throws IOException, DuplicatedPassKeyException {
    var setup = setupPassManagerAndRunSpec(
        "passes/singleResourceWriteValidation/invalid_reg_triple_branch.vadl",
        PassOrders.viam(getConfiguration(false))
            .untilFirst(SingleResourceWriteValidationPass.class)
            .addDump("build/test-out")
    );


    var spec = setup.specification();

    var instr = (Instruction) findDefinitionsByFilter(spec, f -> f instanceof Instruction)
        .stream().findFirst().get();

    var sideEffects = instr.behavior().getNodes(SideEffectNode.class);

//    for (var sideEffect : sideEffects.toList()) {
//      var sb = new StringBuilder();
//      sideEffect.condition().prettyPrint(sb);
//      System.out.println(sb);
//      var conditionCopy = sideEffect.condition().copy();
//      var graph = new Graph("test");
//      graph.addWithInputs(conditionCopy);
//      var dotGraph = new DotGraphVisualizer()
//          .load(graph)
//          .visualize();
//      System.out.println(dotGraph);
//    }


  }

}
