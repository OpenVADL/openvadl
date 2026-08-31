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

package vadl.iss.passes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.Constant;
import vadl.viam.Identifier;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.RegisterRef;
import vadl.viam.Specification;
import vadl.viam.UserModeEmulation;

/**
 * A specialized hardcoded rendering pass for QEMU User-Mode Emulation (UME) source files.
 */
public class UmeHardcodedRiscvDefinitionPass extends AbstractIssPass {
  public UmeHardcodedRiscvDefinitionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("UME Hardcoded RISC-V Definition");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    if (!"rv64ume".equals(viam.simpleName())) {
      return null;
    }

    InstructionSetArchitecture isa = viam.isa()
        .orElseThrow(() -> new IllegalStateException("ISA not found in " + viam.simpleName()));

    Abi abi = viam.abi()
        .orElseThrow(() -> new IllegalStateException("ABI not found in " + viam.simpleName()));

    UserModeEmulation ume = createDummySolution(abi, isa);
    viam.add(ume);
    return null;
  }

  /**
   * Creates a default {@link UserModeEmulation} configuration,
   * pre-configured for the RISC-V architecture.
   * * @return a standard RISC-V user-mode emulation setup.
   */
  public static UserModeEmulation createDummySolution(Abi abi, InstructionSetArchitecture isa) {
    Identifier identifier = new Identifier(new String[]{"ume"},
        SourceLocation.INVALID_SOURCE_LOCATION);

    var registerResource = abi.stackPointer().registerFile();

    List<RegisterRef> args = IntStream.rangeClosed(10, 15)
        .mapToObj(i -> new RegisterRef(
            registerResource,
            List.of(Constant.Value.of(BigInteger.valueOf(i).toByteArray(), DataType.bits(5))),
            SourceLocation.INVALID_SOURCE_LOCATION
        ))
        .toList();

    var syscallInstr = isa.ownInstructions().stream()
        .filter(i -> "ECALL".equals(i.simpleName()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("ECALL instruction not found in ISA"));

    var syscallNrRef = new RegisterRef(
        registerResource,
        List.of(Constant.Value.of(BigInteger.valueOf(17).toByteArray(), DataType.bits(5))),
        SourceLocation.INVALID_SOURCE_LOCATION
    );

    var syscallReturnRef = new RegisterRef(
        registerResource,
        List.of(Constant.Value.of(BigInteger.valueOf(10).toByteArray(), DataType.bits(5))),
        SourceLocation.INVALID_SOURCE_LOCATION
    );

    return new UserModeEmulation(
        identifier,
        isa, abi, args,
        syscallInstr,
        syscallNrRef,
        syscallReturnRef);
  }

}
