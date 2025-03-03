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
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This file contains the logic for lowering stack frames.
 */
public class EmitFrameLoweringCppFilePass extends LcbTemplateRenderingPass {

  public EmitFrameLoweringCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/FrameLowering.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "FrameLowering.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var returnAddress =
        renderRegister(abi.returnAddress().registerFile(), abi.returnAddress().addr());
    var stackAlignment = abi.stackAlignment();
    var transientStackAlignment = abi.transientStackAlignment();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "hasFramePointer", abi.hasFramePointer(),
        "framePointer", framePointer,
        "stackPointer", stackPointer,
        "returnAddress", returnAddress,
        "stackAlignment", stackAlignment.inBytes(),
        "transientStackAlignment", transientStackAlignment.inBytes());
  }
}
