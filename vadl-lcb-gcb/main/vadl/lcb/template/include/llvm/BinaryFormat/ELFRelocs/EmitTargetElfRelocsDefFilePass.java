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

package vadl.lcb.template.include.llvm.BinaryFormat.ELFRelocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file creates the ELF relocation definitions.
 */
public class EmitTargetElfRelocsDefFilePass extends LcbTemplateRenderingPass {

  public EmitTargetElfRelocsDefFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/BinaryFormat/ELFRelocs/Target.def";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/BinaryFormat/ELFRelocs/"
        + lcbConfiguration().targetName().value() + ".def";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var relocations = new ArrayList<>(output.elfRelocations());
    relocations.sort(
        Comparator.comparing(o -> o.elfRelocationName().value()));
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        CommonVarNames.RELOCATIONS, relocations);
  }
}
