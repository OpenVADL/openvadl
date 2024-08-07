package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.types.DataType;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.WriteMemNode;


public class MemoryTest extends AbstractTest {

  @Test
  void testMemory() {
    var spec = runAndGetViamSpecification("memory/valid_memory.vadl");

    var mem = findDefinitionByNameIn("Test::MEM", spec, Memory.class);
    var read_1 = findDefinitionByNameIn("Test::READ_1", spec, Instruction.class);
    var read_2 = findDefinitionByNameIn("Test::READ_2", spec, Instruction.class);
    var write_1 = findDefinitionByNameIn("Test::WRITE_1", spec, Instruction.class);
    var write_2 = findDefinitionByNameIn("Test::WRITE_2", spec, Instruction.class);

    assertEquals(32, mem.wordSize());

    {
      var behavior = read_1.behavior();
      var memRead = behavior.getNodes(ReadMemNode.class).findFirst().get();
      assertEquals(1, memRead.words());
      assertEquals(mem.wordSize(), memRead.type().bitWidth());
    }

    {
      var behavior = read_2.behavior();
      var memRead = behavior.getNodes(ReadMemNode.class).findFirst().get();
      assertEquals(4, memRead.words());
      assertEquals(4 * mem.wordSize(), memRead.type().bitWidth());
    }

    {
      var behavior = write_1.behavior();
      var memWrite = behavior.getNodes(WriteMemNode.class).findFirst().get();
      assertEquals(1, memWrite.words());
      assertEquals(mem.wordSize(), ((DataType) memWrite.value().type()).bitWidth());
    }

    {
      var behavior = write_2.behavior();
      var memWrite = behavior.getNodes(WriteMemNode.class).findFirst().get();
      assertEquals(2, memWrite.words());
      assertEquals(mem.wordSize() * 2, ((DataType) memWrite.value().type()).bitWidth());
    }

  }
}
