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

package vadl.iss.passes;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;

/**
 * Extracts information that is necessary for the
 * {@link vadl.iss.template.gdb_xml.EmitIssGdbXmlPass} and
 * {@link vadl.iss.template.target.EmitIssGdbStubPass} in order to support GDB debugging
 * on the generated QEMU target.
 * It finds all registers and register files and provides a list of them, containing all required
 * information.
 */
public class IssGdbInfoExtractionPass extends AbstractIssPass {

  public IssGdbInfoExtractionPass(IssConfiguration configuration) {
    super(configuration);
  }

  /**
   * Contains a list of registers that can be accessed via GDB.
   */
  public record Result(
      List<Reg> regs
  ) {

    /**
     * A ready to render register representation with the name, bitSize, GDB register type,
     * and register file index (if it was from a register file).
     */
    public record Reg(
        String name,
        int bitSize,
        String type,
        // only used if the origin is a register file
        int fileIndex,
        Resource origin
    ) implements Renderable {
      @Override
      public Map<String, Object> renderObj() {
        return Map.of(
            "name", name,
            "bitSize", bitSize,
            "type", type,
            "fileIndex", fileIndex
        );
      }
    }
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS GDB Info Extraction Pass");
  }

  @Nullable
  @Override
  public Result execute(PassResults passResults, Specification viam) throws IOException {
    var isa = viam.mip().get().isa();
    var regFiles = isa.ownRegisterFiles().stream().flatMap(e ->
        IntStream.range(0, e.numberOfRegisters())
            .mapToObj(i -> getRegOfFile(e, i))
    );
    var regs = isa.ownRegisters().stream().map(r -> getReg(r, viam));

    return new Result(
        Streams.concat(regFiles, regs).toList()
    );
  }

  private Result.Reg getRegOfFile(RegisterFile registerFile, int i) {
    // TODO: Determine from ABI if it exists
    var name = (registerFile.simpleName() + i).toLowerCase();
    return new Result.Reg(
        name,
        registerFile.resultType().bitWidth(),
        "int",
        i,
        registerFile
    );
  }

  private Result.Reg getReg(Register register, Specification viam) {
    // TODO: Determine from ABI if it exists
    var name = register.simpleName().toLowerCase();

    var pc = requireNonNull(viam.mip().get().isa().pc());
    // TODO: Also determine from ABI (also check for data pointer)
    var isCodePtr = register == pc.registerTensor();

    return new Result.Reg(
        name,
        register.resultType().bitWidth(),
        isCodePtr ? "code_ptr" : "int",
        0, // not used for registers
        register
    );
  }
}

