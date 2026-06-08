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
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.Abi;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;
import vadl.viam.RegisterRef;
import vadl.viam.Specification;
import vadl.viam.UserModeEmulation;
import vadl.viam.graph.Graph;

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

    var registerResource = isa.registerTensors()
        .stream()
        .filter(r -> r.simpleName().equals("X"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Register file X not found in ISA"));

    var index = Constant.Value.of(BigInteger.valueOf(10).toByteArray(), DataType.bits(5));

    var ref = new RegisterRef(
        registerResource,
        List.of(index),
        SourceLocation.INVALID_SOURCE_LOCATION
    );

    List<RegisterRef> args = List.of(ref);

    Graph emptyGraph = new Graph("empty_graph");

    BitsType mockType = BitsType.bits(32);

    Function mockFunc = new Function(
        new Identifier(new String[]{"dummy_function"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        new Parameter[0],
        Type.string(),
        emptyGraph
    );

    Assembly emptyAssembly = new Assembly(new Identifier(new String[]{"dummy_assembly"},
        SourceLocation.INVALID_SOURCE_LOCATION), mockFunc);

    Format dummyFormat = new Format(new Identifier(new String[]{"dummy_format"},
        SourceLocation.INVALID_SOURCE_LOCATION), mockType);

    Encoding emptyEncoding = new Encoding(
        new Identifier(new String[]{"dummy_encoding"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        dummyFormat, new Encoding.Field[0]);

    Instruction mockSyscallInsn = new Instruction(
        new Identifier(new String[]{"ECALL"},
            SourceLocation.INVALID_SOURCE_LOCATION),
        emptyGraph, emptyAssembly, emptyEncoding
    );

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
        mockSyscallInsn,
        syscallNrRef,
        syscallReturnRef);
  }

}
