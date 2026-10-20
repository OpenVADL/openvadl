// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.llvmLowering.RegisterTypesCtx;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Specification;

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

  record CCIfType(ValueType type, List<String> registerRefs) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of("type", type.getLlvmType(),
          "registerRefs", String.join(",", registerRefs));
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi = specification.abi().orElseThrow();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "calleeRegisters", abi.calleeSaved().stream().map(this::renderRegister).toList(),
        "functionRegisters", ifTypesFromRegisters(abi.argumentRegisters()),
        "returnIfTypes", ifTypesFromRegisters(abi.returnRegisters()));
  }

  private List<CCIfType> ifTypesFromRegisters(List<List<Abi.AbiRegister>> definitions) {
    Map<ValueType, Set<String>> regsByType = new LinkedHashMap<>();

    for (var def : definitions) {
      for (var reg : def) {
        var ty =
            reg.registerFile().expectExtension(RegisterTypesCtx.class).valueTypes().getFirst();
        regsByType.computeIfAbsent(ty, ignored -> new HashSet<>())
            .add(renderRegister(reg));
      }
    }

    return regsByType.entrySet().stream()
        .map(entry ->
            new CCIfType(
                entry.getKey(),
                entry.getValue().stream().sorted().toList()
            )
        )
        .toList();
  }
}
