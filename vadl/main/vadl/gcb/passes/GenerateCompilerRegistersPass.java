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

package vadl.gcb.passes;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.CompilerRegisterClass;
import vadl.gcb.valuetypes.GeneralCompilerRegister;
import vadl.gcb.valuetypes.IndexedCompilerRegister;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * The VIAM gives us register files, but we need separate registers.
 * The RISC-V specification has the register file {@code X}. This pass creates
 * the registers {@code X0} to {@code X31} from this register file.
 */
public class GenerateCompilerRegistersPass extends Pass {
  public GenerateCompilerRegistersPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateRegistersPass");
  }

  /**
   * Output of the pass.
   */
  public record Output(
      List<CompilerRegister> generalRegisters,
      List<CompilerRegisterClass> registerClasses
  ) {

  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    var generalRegisters = generalRegisters(viam.registerTensors()
        .filter(RegisterTensor::isSingleRegister).toList());
    int dwarfOffset = generalRegisters.size();
    var registerClasses =
        registerClasses(viam.registerTensors().filter(RegisterTensor::isRegisterFile)
                .toList(), abi,
            dwarfOffset);

    return new Output(generalRegisters, registerClasses);
  }

  private List<CompilerRegister> generalRegisters(List<RegisterTensor> registers) {
    var compilerRegisters = new ArrayList<CompilerRegister>();
    int dwarfOffset = 0;

    for (var register : registers) {
      // The alias should be the same as the register name.
      var alias = GeneralCompilerRegister.generateName(register);

      var compilerRegister =
          new GeneralCompilerRegister(register, alias, Collections.emptyList(), dwarfOffset++);
      compilerRegisters.add(compilerRegister);
    }

    return compilerRegisters;
  }

  private List<CompilerRegisterClass> registerClasses(List<RegisterTensor> registerFiles,
                                                      Abi abi,
                                                      int dwarfOffset) {
    var result = new ArrayList<CompilerRegisterClass>();

    for (var registerFile : registerFiles) {
      var registers = IndexedCompilerRegister.fromRegisterFile(registerFile, abi, dwarfOffset);
      dwarfOffset += registers.size();

      var alignment = ensureNonNull(abi.registerFileAlignment().get(registerFile),
          () -> Diagnostic.error("There is not alignment for the register file defined",
              registerFile.sourceLocation().join(abi.sourceLocation())));

      result.add(new CompilerRegisterClass(registerFile, registers, alignment));
    }

    return result;
  }
}
