// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;

public class ArtificialResPartialAccessExpansionPassTest extends AbstractTest {

  @Test
  void frontendKeepsVectorBenchTensorWriteAsPartialAliasWrite() throws IOException {
    var spec = runAndGetViamSpecification("sys/vectorbench/vectorbench64.vadl");
    var instr = TestUtils.findDefinitionByNameIn("VectorBench64::VADD_TENSOR_VV", spec,
        Instruction.class);

    var aliasWrites = instr.behavior().getNodes(WriteArtificialResNode.class)
        .filter(write -> write.resourceDefinition().simpleName().equals("V"))
        .toList();
    assertEquals(1, aliasWrites.size());
    assertEquals(1, aliasWrites.getFirst().indices().size());
    assertEquals(1024, aliasWrites.getFirst().value().type().asDataType().bitWidth());
  }

  @Test
  void frontendKeepsVectorBenchStoreAsPartialAliasRead() throws IOException {
    var spec = runAndGetViamSpecification("sys/vectorbench/vectorbench64.vadl");
    var instr = TestUtils.findDefinitionByNameIn("VectorBench64::VST", spec, Instruction.class);

    var aliasReads = instr.behavior().getNodes(ReadArtificialResNode.class)
        .filter(read -> read.resourceDefinition().simpleName().equals("V"))
        .toList();
    assertEquals(1, aliasReads.size());
    assertEquals(1, aliasReads.getFirst().indices().size());
    assertEquals(1024, aliasReads.getFirst().type().bitWidth());
  }

  @Test
  void expansionPassRestoresConcreteVectorBenchAliasAccesses()
      throws IOException, DuplicatedPassKeyException {
    var setup = setupPassManagerAndRunSpec(
        "sys/vectorbench/vectorbench64.vadl",
        PassOrders.viam(getConfiguration(false))
            .untilFirst(ArtificialResPartialAccessExpansionPass.class)
    );
    var spec = setup.specification();

    var tensorInstr = TestUtils.findDefinitionByNameIn("VectorBench64::VADD_TENSOR_VV", spec,
        Instruction.class);
    assertEquals(32, tensorInstr.behavior().getNodes(WriteArtificialResNode.class)
        .filter(write -> write.resourceDefinition().simpleName().equals("V"))
        .count());
    assertEquals(0, tensorInstr.behavior().getNodes(WriteArtificialResNode.class)
        .filter(write -> write.resourceDefinition().simpleName().equals("V"))
        .filter(write -> write.indices().size() < write.resourceDefinition().dimensions().size())
        .count());

    var storeInstr = TestUtils.findDefinitionByNameIn("VectorBench64::VST", spec,
        Instruction.class);
    assertEquals(32, storeInstr.behavior().getNodes(ReadArtificialResNode.class)
        .filter(read -> read.resourceDefinition().simpleName().equals("V"))
        .count());
    assertEquals(0, storeInstr.behavior().getNodes(ReadArtificialResNode.class)
        .filter(read -> read.resourceDefinition().simpleName().equals("V"))
        .filter(read -> read.indices().size() < read.resourceDefinition().dimensions().size())
        .count());
  }
}
