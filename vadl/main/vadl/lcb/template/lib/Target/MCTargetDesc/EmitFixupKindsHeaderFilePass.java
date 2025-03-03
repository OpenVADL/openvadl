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

package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file defines the fixups.
 */
public class EmitFixupKindsHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitFixupKindsHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetFixupKinds.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "FixupKinds.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var fixups = output.fixups();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "fixups", fixups.stream().map(this::map).toList());
  }

  private Map<String, Object> map(Fixup obj) {
    return Map.of(
        "name", obj.name().value()
    );
  }
}
