package vadl.test.iss.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.test.TestUtils.findDefinitionByNameIn;
import static vadl.utils.GraphUtils.getSingleNode;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.types.BuiltInTable;
import vadl.viam.Instruction;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadRegNode;

/**
 * We need this tests as there is no occurrence of the tested problems in the RISC-V specification.
 */
public class IssTcgSchedulingPassTest extends AbstractTest {

  @Test
  public void test_schedule_in_both_branches() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), true));
    // var config = getConfiguration(false);

    var setup = setupPassManagerAndRunSpec("passes/issTcgScheduling/valid_branch_1.vadl",
        PassOrders.iss(config)
    );
    var viam = setup.specification();

    var test1 = findDefinitionByNameIn("ValidBranch::TEST1", viam, Instruction.class);

    var regRead = getSingleNode(test1.behavior(), ReadRegNode.class);

    var builtInCalls = test1.behavior().getNodes(BuiltInCall.class)
        .filter(b -> b.builtIn() == BuiltInTable.ADD)
        .toList();
    assertEquals(1, builtInCalls.size());

    var addition = builtInCalls.get(0);

    var regSchedules = regRead.usages().filter(ScheduledNode.class::isInstance).toList();
    var addSchedules = addition.usages().filter(ScheduledNode.class::isInstance).toList();

    assertEquals(2, addSchedules.size());
    assertEquals(2, addSchedules.size());

    var branchBegins = getSingleNode(test1.behavior(), IfNode.class)
        .branches();

    for (var begin : branchBegins) {
      var beginSucc = begin.next();
      assertTrue(regSchedules.contains(beginSucc));
    }

    for (var regSch : regSchedules) {
      var regSchNext = ((ScheduledNode) regSch).next();
      assertTrue(addSchedules.contains(regSchNext));
    }

  }

}
