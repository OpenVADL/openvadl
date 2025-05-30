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

package vadl.lcb.riscv.riscv64;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.llvmLowering.GenerateTableGenRegistersPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;

public class GenerateRegisterClassesRiscv64PassTest extends AbstractLcbTest {

  @ParameterizedTest
  @CsvSource({"processorNameValue,X,32,i64"})
  void shouldHaveMainRegisterClasses(String namespace, String name, int alignment, String type)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateTableGenRegistersPass.class.getName()));
    var passManager = setup.passManager();

    // When
    var generatedRegisterClasses =
        (GenerateTableGenRegistersPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateTableGenRegistersPass.class);

    // Then
    var rg = generatedRegisterClasses.registerClasses().stream().filter(x -> x.name().equals(name))
        .findFirst();
    Assertions.assertTrue(rg.isPresent());
    Assertions.assertEquals(namespace, rg.get().namespace().value());
    Assertions.assertEquals(name, rg.get().name());
    Assertions.assertEquals(alignment, rg.get().alignment());
    Assertions.assertEquals(type, rg.get().regTypes().get(0).getLlvmType());
  }


  @ParameterizedTest
  @CsvSource({"X,X0",
      "X,X1",
      "X,X2",
      "X,X3",
      "X,X4",
      "X,X5",
      "X,X6",
      "X,X7",
      "X,X8",
      "X,X9",
      "X,X10",
      "X,X11",
      "X,X12",
      "X,X13",
      "X,X14",
      "X,X15",
      "X,X16",
      "X,X17",
      "X,X18",
      "X,X19",
      "X,X20",
      "X,X21",
      "X,X22",
      "X,X23",
      "X,X24",
      "X,X25",
      "X,X26",
      "X,X27",
      "X,X28",
      "X,X29",
      "X,X30",
      "X,X31"
  })
  void shouldHaveCorrectRegisterAssignments(String name, String reg)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateTableGenRegistersPass.class.getName()));
    var passManager = setup.passManager();
    var spec = setup.specification();

    // When
    var generatedRegisterClasses =
        (GenerateTableGenRegistersPass.Output) passManager.getPassResults()
            .lastResultOf(GenerateTableGenRegistersPass.class);

    // Then
    var rg = generatedRegisterClasses.registerClasses().stream().filter(x -> x.name().equals(name))
        .findFirst();
    Assertions.assertTrue(rg.isPresent());
    var x = rg.get().registers().stream().filter(y -> y.compilerRegister().name().equals(reg))
        .findFirst();
    Assertions.assertTrue(x.isPresent(), "Register was not found");
  }
}
