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

package vadl.lcb.passes.llvmLowering.immediates;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This pass extracts the immediates from the TableGen records.
 */
public class GenerateTableGenImmediateRecordPass extends Pass {

  public GenerateTableGenImmediateRecordPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateTableGenImmediateRecordPass");
  }

  @Nullable
  @Override
  public List<TableGenImmediateRecord> execute(PassResults passResults,
                                               Specification viam) throws IOException {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    var immediates = new ArrayList<TableGenImmediateRecord>();

    viam.isa().orElseThrow()
        .ownInstructions().forEach(instruction -> {
          instruction.format().fieldAccesses().forEach(fieldAccess -> {
            var originalType = abi.stackPointer().registerFile().resultType();
            var llvmType = ValueType.from(originalType);

            if (llvmType.isEmpty()) {
              var upcastedType = CppTypeMap.upcast(originalType);
              var upcastedValueType =
                  ensurePresent(ValueType.from(upcastedType), () -> Diagnostic.error(
                      "Compiler generator was not able to change the type to the architecture's "
                          + "bit width: " + upcastedType.toString(),
                      fieldAccess.location()));
              immediates.add(new TableGenImmediateRecord(instruction,
                  fieldAccess,
                  upcastedValueType));
            } else {
              immediates.add(new TableGenImmediateRecord(instruction,
                  fieldAccess,
                  llvmType.get()));
            }
          });
        });

    return immediates;
  }
}
