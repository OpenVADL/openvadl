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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.RegisterTensor;
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
        // same as index in XML
        int gdbNr,
        int bitSize,
        String type,
        // only used if the origin is a register file
        List<Integer> fileIndices,
        RegisterTensor origin
    ) implements Renderable {
      @Override
      public Map<String, Object> renderObj() {
        return Map.of(
            "name", name,
            "bitSize", bitSize,
            "type", type,
            "fileIndices", fileIndices
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
    var pc = requireNonNull(isa.pc()).registerTensor();

    AtomicInteger i = new AtomicInteger();
    var res = new ArrayList<Result.Reg>();
    for (var reg : isa.registerTensors()) {
      getRegTensor(reg, i.get(), pc).forEach(r -> {
        res.add(r);
        i.getAndIncrement();
      });
    }

    return new Result(res);
  }

  private Stream<Result.Reg> getRegTensor(RegisterTensor reg, int i, RegisterTensor pc) {
    var idxDimSizes = reg.indexDimensions().stream().map(RegisterTensor.Dimension::size).toList();
    var regs = regIndexes(idxDimSizes);
    var isCodePtr = reg == pc;
    return regs.stream().map(regIndices -> {
      var idxNames = regIndices.stream().map(ri -> "" + ri).collect(Collectors.joining("_"));
      return new Result.Reg(
          reg.simpleName().toLowerCase() + idxNames,
          i,
          reg.resultType(reg.maxNumberOfAccessIndices()).bitWidth(),
          isCodePtr ? "code_ptr" : "int",
          regIndices,
          reg
      );
    });
  }

  /**
   * Generates a list of enumerations based on the given dimensions.
   * E.g. { 2, 3 } would give a list of { (0, 0), (0, 1), (0, 2), (1, 0), (1, 1), (1, 2) }.
   */
  private List<List<Integer>> regIndexes(List<Integer> dimensions) {
    List<List<Integer>> res = new ArrayList<>();
    res.add(new ArrayList<>());                 // start with empty prefix
    for (int dim : dimensions) {                // outer‑to‑inner order
      List<List<Integer>> next = new ArrayList<>();
      for (List<Integer> prefix : res) {      // extend every prefix
        for (int i = 0; i < dim; i++) {
          List<Integer> tuple = new ArrayList<>(prefix);
          tuple.add(i);                   // add current index
          next.add(tuple);
        }
      }
      res = next;                             // move on to next dimension
    }
    return res;
  }
}

