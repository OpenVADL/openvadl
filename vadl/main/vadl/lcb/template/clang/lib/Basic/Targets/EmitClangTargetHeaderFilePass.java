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

package vadl.lcb.template.clang.lib.Basic.Targets;

import static vadl.lcb.template.utils.DataLayoutProvider.createDataLayout;
import static vadl.lcb.template.utils.DataLayoutProvider.createDataLayoutString;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * This file contains the datatype configuration for the llvm's types.
 */
public class EmitClangTargetHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitClangTargetHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/Targets/ClangTarget.h";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/Targets/" + lcbConfiguration().targetName().value() + ".h";
  }

  record ClangType(String name, String value) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "value", value
      );
    }
  }


  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var gpr = ensurePresent(
        specification.registerTensors().filter(RegisterTensor::isRegisterFile).findFirst(),
        "Specification requires at least one register file");

    var abi = specification.abi().orElseThrow();
    var types = abi.clangTypes().stream().map(x -> new ClangType(x.typeName(), x.value()))
        .toList();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        CommonVarNames.DATALAYOUT, createDataLayoutString(createDataLayout(gpr)),
        "clangTypes", types);
  }
}
