// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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
