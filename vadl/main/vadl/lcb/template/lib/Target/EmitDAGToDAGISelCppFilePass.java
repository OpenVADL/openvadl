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

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * This file contains the transformation from DAG to InstructionSelectionDag.
 */
public class EmitDAGToDAGISelCppFilePass extends LcbTemplateRenderingPass {

  public EmitDAGToDAGISelCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/DAGToDAGISel.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "DAGToDAGISel.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var stackPointerType =
        ValueType.from(abi.framePointer().registerFile().resultType()).get().getLlvmType();

    // Register files with a zero constraint.
    var registerFilesCandidates = specification.isa().orElseThrow()
        .registerTensors()
        .stream()
        .filter(RegisterTensor::isRegisterFile)
        .filter(x -> x.zeroRegister().isPresent())
        .toList();

    // The idea is that when we have zero register then we can use it.
    // So we can use RR instructions and not only RI.
    // This only works when we know that there is only one register file. Otherwise,
    // we might get a problem.
    if (registerFilesCandidates.size() == 1) {
      var registerFile = registerFilesCandidates.stream().findFirst().get();
      var zeroIndex = registerFile.zeroRegister().get().getFirst().intValue();
      var zeroRegister = registerFile.generateRegisterFileName(zeroIndex);

      return Map.of(CommonVarNames.NAMESPACE,
          lcbConfiguration().targetName().value().toLowerCase(),
          "replaceImmZeroByRegisterZero", true,
          "zeroRegister", zeroRegister,
          "stackPointerType", stackPointerType
      );
    } else {
      return Map.of(CommonVarNames.NAMESPACE,
          lcbConfiguration().targetName().value().toLowerCase(),
          "replaceImmZeroByRegisterZero", false,
          "zeroRegister", "",
          "stackPointerType", stackPointerType);
    }
  }
}
