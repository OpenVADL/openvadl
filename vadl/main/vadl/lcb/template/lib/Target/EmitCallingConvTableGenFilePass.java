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

package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Specification;
import vadl.viam.passes.dummyPasses.DummyAbiPass;

/**
 * This file contains the calling conventions for the defined backend.
 */
public class EmitCallingConvTableGenFilePass extends LcbTemplateRenderingPass {
  public EmitCallingConvTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/CallingConv.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "CallingConv.td";
  }

  record AssignToReg(String type, String registerRefs) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "type", type,
          "registerRefs", registerRefs
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi = (Abi) passResults.lastResultOf(DummyAbiPass.class);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "calleeRegisters", abi.calleeSaved().stream().map(Abi.RegisterRef::render).toList(),
        "functionRegisterType", getFuncArgsAssignToReg(abi).type,
        "functionRegisters", getFuncArgsAssignToReg(abi),
        "returnRegisters", getReturnAssignToReg(abi));
  }

  @Nonnull
  private AssignToReg getReturnAssignToReg(Abi abi) {
    ensure(abi.returnRegisters().stream().map(x -> x.registerFile().relationType()).collect(
            Collectors.toSet()).size() == 1,
        "All return registers must have the same type and at least one must exist");
    return new AssignToReg(
        ValueType.from(abi.returnRegisters().get(0).registerFile().resultType()).get()
            .getLlvmType(),
        abi.returnRegisters().stream().map(Abi.RegisterRef::render)
            .collect(Collectors.joining(", ")));
  }

  @Nonnull
  private AssignToReg getFuncArgsAssignToReg(Abi abi) {
    ensure(abi.argumentRegisters().stream().map(x -> x.registerFile().relationType()).collect(
            Collectors.toSet()).size() == 1,
        "All function argument registers must have the same type and at least one must exist");
    return new AssignToReg(
        ValueType.from(abi.argumentRegisters().get(0).registerFile().resultType()).get()
            .getLlvmType(),
        abi.argumentRegisters().stream().map(Abi.RegisterRef::render)
            .collect(Collectors.joining(", ")));
  }
}
