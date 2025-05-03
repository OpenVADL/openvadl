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

import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.VariantKind;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.BaseInfoFunctionProvider;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Specification;

/**
 * This file contains the logic for emitting MC operands.
 */
public class EmitMCExprCppFilePass extends LcbTemplateRenderingPass {
  public EmitMCExprCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCExpr.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCExpr.cpp";
  }

  record DecodeMapping(String variantKind, String decodeFunction) implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "variantKind", variantKind,
          "decodeFunction", decodeFunction
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var variantKinds = output.variantKinds().stream().map(x -> Map.of(
        "human", x.human(),
        "value", x.value()
    )).toList();

    var immediates = output.variantKinds()
        .stream()
        .filter(VariantKind::isImmediate)
        .map(VariantKind::value)
        .toList();


    var baseInfos = BaseInfoFunctionProvider.getBaseInfoRecords(passResults);
    var decodeMappings = decodeMappings(passResults, specification, output);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "immediates", immediates,
        "variantKinds", variantKinds,
        "mappingVariantKindsIntoBaseInfos", baseInfos,
        "decodeMappings", decodeMappings,
        "pltVariantKindName", VariantKind.plt().value()
    );
  }

  /**
   * For each field access we generate a mapping from the variant kind to the decode function.
   * This is necessary for pseudo expansion. The {@code MCInstExpander} sets the variant kind
   * for immediate operands and then the {@code MCCodeEmitter} calls into {@code MCExpr} to
   * apply the correct decode function for the variant kind.
   * see: {@link EmitMCInstExpanderCppFilePass}, {@link EmitMCCodeEmitterCppFilePass}
   */
  List<DecodeMapping> decodeMappings(PassResults passResults, Specification specification,
                                     GenerateLinkerComponentsPass.Output output) {

    var decodeVariantKinds = output.variantKindStore().decodeVariantKinds();
    var decodeFunctions = ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults);

    var fieldAccesses = specification.isa().map(isa -> isa.ownFormats().stream()).orElseGet(
        Stream::empty).flatMap(formats -> formats.fieldAccesses().stream());

    return fieldAccesses.map(
        fieldAccess -> {
          var variantKind = decodeVariantKinds.get(fieldAccess);
          var decodeFunction = decodeFunctions.get(fieldAccess.fieldRef());
          ensure(variantKind != null, "No variant kind found for field access: %s", fieldAccess);
          ensure(decodeFunction != null,
              "No decode function found for field access: %s", fieldAccess);
          return new DecodeMapping(variantKind.value(), decodeFunction.functionName().lower());
        }).toList();
  }
}
