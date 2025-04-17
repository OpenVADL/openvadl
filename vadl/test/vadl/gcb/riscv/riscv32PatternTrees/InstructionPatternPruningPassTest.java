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

package vadl.gcb.riscv.riscv32PatternTrees;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.gcb.AbstractGcbTest;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.dependency.SelectNode;

class InstructionPatternPruningPassTest extends AbstractGcbTest {

  @Test
  void shouldPruneAddDiv1()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
      instruction ADDDIV1 : Rtype =                        // 3 register operand instructions
        X(rd) :=
          if rs2 = 0 then
            0 as Regs
          else
            (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
    encoding ADDDIV1 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
    assembly ADDDIV1 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */

    // Then
    var addDiv1 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV1"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(addDiv1).isPresent();
    Assertions.assertThat(addDiv1.get().getNodes(SelectNode.class)).isEmpty();
  }

  @Test
  void shouldPruneAddDiv2()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
       instruction ADDDIV2 : Rtype =                        // 3 register operand instructions
        X(rd) :=
          if rs2 != 0 then
            (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
          else
            0 as Regs
       encoding ADDDIV2 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
       assembly ADDDIV2 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */

    // Then
    var addDiv2 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV2"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(addDiv2).isPresent();
    Assertions.assertThat(addDiv2.get().getNodes(SelectNode.class)).isEmpty();
  }

  @Test
  void shouldPruneAddDiv3()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
       instruction ADDDIV3 : Rtype =                        // 3 register operand instructions
           X(rd) :=
             if rs1 != 0 then
               (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
             else
               0 as Regs
       encoding ADDDIV3 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
       assembly ADDDIV3 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */

    // Then
    var addDiv3 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV3"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(addDiv3).isPresent();
    Assertions.assertThat(addDiv3.get().getNodes(SelectNode.class)).isEmpty();
  }

  @Test
  void shouldPruneAddDiv4()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
       instruction ADDDIV4 : Rtype =                        // 3 register operand instructions
              X(rd) :=
                if rs1 != 0 & rs2 != 0 then
                  (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
                else
                  0 as Regs
          encoding ADDDIV4 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
          assembly ADDDIV4 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */


    // Then
    var adddiv4 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV4"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(adddiv4).isPresent();
    Assertions.assertThat(adddiv4.get().getNodes(SelectNode.class)).isEmpty();
  }

  @Test
  void shouldNotPruneAddDiv5()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
       instruction ADDDIV5 : Rtype =                        // 3 register operand instructions
              X(rd) :=
                if rs1 = 0 & rs2 != 0 then
                  (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
                else
                  0 as Regs
          encoding ADDDIV5 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
          assembly ADDDIV5 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */


    // Then
    var adddiv5 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV5"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(adddiv5).isPresent();
    Assertions.assertThat(adddiv5.get().getNodes(SelectNode.class)).isNotEmpty();
  }

  @Test
  void shouldPruneAddDiv6()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
     instruction ADDDIV6 : Rtype =
      if rs2 = 0 then
          raise Exc
      else
          X(rd) := (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
      encoding ADDDIV6 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
      assembly ADDDIV6 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */


    // Then
    var adddiv6 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV6"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(adddiv6).isPresent();
    Assertions.assertThat(adddiv6.get()
            .getNodes(Set.of(IfNode.class, MergeNode.class, BeginNode.class, BranchEndNode.class)))
        .isEmpty();
  }

  @Test
  void shouldPruneAddDiv7()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcbAndCppCodeGen(getConfiguration(false), "lcb/riscv32_pattern_trees.vadl");

    /*
    instruction ADDDIV7 : Rtype =
      if rs2 != 0 then
        X(rd) := (((X(rs1) as Bits) + (X(rs2) as Bits)) / (X(rs2) as Bits)) as Regs
      else
        raise Exc
      encoding ADDDIV7 = { opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0001 }
      assembly ADDDIV7 = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
     */

    // Then
    var adddiv6 = setup.specification().isa().map(x -> x.ownInstructions().stream())
        .orElse(Stream.empty())
        .filter(instruction -> instruction.simpleName().equals("ADDDIV7"))
        .map(Instruction::behavior).findFirst();

    Assertions.assertThat(adddiv6).isPresent();
    Assertions.assertThat(adddiv6.get()
            .getNodes(Set.of(IfNode.class, MergeNode.class, BeginNode.class, BranchEndNode.class)))
        .isEmpty();
  }

}