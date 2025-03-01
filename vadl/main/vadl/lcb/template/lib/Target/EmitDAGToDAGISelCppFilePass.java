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

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.WriteRegFileNode;

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
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "DAGToDAGISel.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var labelledInstructions =
        ensureNonNull(
            (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
                IsaMachineInstructionMatchingPass.class), "labelling must be present")
            .labels();
    var lui =
        ensurePresent(
            Objects.requireNonNull(labelledInstructions)
                .getOrDefault(MachineInstructionLabel.LUI, Collections.emptyList())
                .stream().findFirst(),
            () -> Diagnostic.error("Expected an instruction of load upper immediate",
                specification.sourceLocation()));
    var registerFile =
        ensurePresent(
            lui.behavior().getNodes(WriteRegFileNode.class)
                .map(WriteRegFileNode::registerFile)
                .findFirst(),
            () -> Diagnostic.error("Cannot find a register for load upper immediate",
                lui.sourceLocation()));
    var zero = ensurePresent(
        Arrays.stream(registerFile.constraints()).filter(x -> x.value().intValue() == 0)
            .findFirst(),
        () -> Diagnostic.error("Cannot find a zero constraint", registerFile.sourceLocation()));
    var zeroRegister = registerFile.identifier.simpleName() + zero.address().intValue();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "lui", lui.identifier.simpleName(),
        "zeroRegister", zeroRegister,
        "stackPointerType",
        ValueType.from(abi.framePointer().registerFile().resultType()).get().getLlvmType());
  }
}
