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

package vadl.gcb.riscv.riscv32;

import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BuiltInTable;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.StructGetFieldNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

class InstructionRegisterEliminationPassTest extends AbstractLcbTest {

  @Test
  void shouldNotRemoveNZCV_Z()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "lcb/riscv32_register_elimination.vadl");


    // Then
    var temp = setup.specification().isa().stream().flatMap(x -> x.ownInstructions().stream())
        .filter(instruction -> instruction.simpleName().equals("TEMP"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(temp).isPresent();
    Assertions.assertThat(temp.get().getNodes(ReadRegTensorNode.class)
        .filter(x -> x.regTensor().identifier.simpleName().equals("NZCV_Z"))).isNotEmpty();
  }

  @Test
  void shouldRemoveNZCV_N()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "lcb/riscv32_register_elimination.vadl");

    // Then
    var temp = setup.specification().isa().stream().flatMap(x -> x.ownInstructions().stream())
        .filter(instruction -> instruction.simpleName().equals("TEMP2"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(temp).isPresent();
    Assertions.assertThat(temp.get().getNodes(WriteRegTensorNode.class)
        .filter(x -> x.regTensor().identifier.simpleName().equals("NZCV_N"))).isEmpty();
  }

  @Test
  void shouldNotRemoveNZCV_N_whenTemp3()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "lcb/riscv32_register_elimination.vadl");

    // Then
    var temp = setup.specification().isa().stream().flatMap(x -> x.ownInstructions().stream())
        .filter(instruction -> instruction.simpleName().equals("TEMP3"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(temp).isPresent();
    Assertions.assertThat(temp.get().getNodes(ReadRegTensorNode.class)
        .filter(x -> x.regTensor().identifier.simpleName().equals("NZCV_N"))).isNotEmpty();
  }

  @Test
  void shouldRemoveNZCV_N_whenTemp4()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "lcb/riscv32_register_elimination.vadl");

    // Then
    var temp = setup.specification().isa().stream().flatMap(x -> x.ownInstructions().stream())
        .filter(instruction -> instruction.simpleName().equals("TEMP4"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(temp).isPresent();
    Assertions.assertThat(temp.get().getNodes(ReadRegTensorNode.class)
        .filter(x -> x.regTensor().identifier.simpleName().equals("NZCV_N"))).isEmpty();
    Assertions.assertThat(temp.get().getNodes(StructGetFieldNode.class)).isEmpty();
    Assertions.assertThat(
            temp.get().getNodes(BuiltInCall.class).filter(x -> x.builtIn().isStatusBuiltin()))
        .isEmpty();
    Assertions.assertThat(
            temp.get().getNodes(BuiltInCall.class).filter(x -> x.builtIn() == BuiltInTable.ADD))
        .isNotEmpty();
  }
}