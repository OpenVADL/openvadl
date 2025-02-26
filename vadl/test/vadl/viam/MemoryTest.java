package vadl.viam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.types.DataType;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;


public class MemoryTest extends AbstractTest {

  @Test
  void testMemory() {
    var spec = runAndGetViamSpecification("unit/memory/valid_memory.vadl");

    var mem = TestUtils.findDefinitionByNameIn("Test::MEM", spec, Memory.class);
    var read_1 = TestUtils.findDefinitionByNameIn("Test::READ_1", spec, Instruction.class);
    var read_2 = TestUtils.findDefinitionByNameIn("Test::READ_2", spec, Instruction.class);
    var write_1 = TestUtils.findDefinitionByNameIn("Test::WRITE_1", spec, Instruction.class);
    var write_2 = TestUtils.findDefinitionByNameIn("Test::WRITE_2", spec, Instruction.class);

    Assertions.assertEquals(32, mem.wordSize());

    {
      var behavior = read_1.behavior();
      var memRead = behavior.getNodes(ReadMemNode.class).findFirst().get();
      Assertions.assertEquals(1, memRead.words());
      Assertions.assertEquals(mem.wordSize(), memRead.type().bitWidth());
    }

    {
      var behavior = read_2.behavior();
      var memRead = behavior.getNodes(ReadMemNode.class).findFirst().get();
      Assertions.assertEquals(4, memRead.words());
      Assertions.assertEquals(4 * mem.wordSize(), memRead.type().bitWidth());
    }

    {
      var behavior = write_1.behavior();
      var memWrite = behavior.getNodes(WriteMemNode.class).findFirst().get();
      Assertions.assertEquals(1, memWrite.words());
      Assertions.assertEquals(mem.wordSize(), ((DataType) memWrite.value().type()).bitWidth());
    }

    {
      var behavior = write_2.behavior();
      var memWrite = behavior.getNodes(WriteMemNode.class).findFirst().get();
      Assertions.assertEquals(2, memWrite.words());
      Assertions.assertEquals(mem.wordSize() * 2, ((DataType) memWrite.value().type()).bitWidth());
    }

  }
}
