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

package vadl.lcb.template.lib.Target.Utils;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.BaseInfoFunctionProvider;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file is a helper class.
 */
public class EmitBaseInfoFilePass extends LcbTemplateRenderingPass {

  public EmitBaseInfoFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Utils/BaseInfo.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/Utils/"
        + processorName + "BaseInfo.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var modifiers = output.modifiers();
    var linkModifierToRelocation = output
        .linkModifierToRelocation()
        .stream()
        .filter(distinctByKey(x -> x.left().value()))
        .map(x -> Map.of("modifier", x.left(),
            "relocation", new ValueRelocationFunctionCodeGenerator(x.right(),
                x.right().valueRelocation()).genFunctionName()))
        .toList();
    var relocations = BaseInfoFunctionProvider.getBaseInfoRecords(passResults);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "isBigEndian", false,
        "relocations", relocations,
        "modifiers", modifiers,
        "linkModifierToRelocation", linkModifierToRelocation
    );
  }
}
