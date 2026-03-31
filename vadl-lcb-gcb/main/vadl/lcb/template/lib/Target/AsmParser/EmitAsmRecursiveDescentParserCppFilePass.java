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

package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.assembly.AssemblyParserCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmParserCaseSensitive;

/**
 * This file includes the definitions for the asm parser.
 */
public class EmitAsmRecursiveDescentParserCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmRecursiveDescentParserCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().targetName().value()
        + "/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var grammarRules = grammarRules(specification);
    var compareFunction = stringCompareFunction(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "asmDescriptionExists", specification.assemblyDescription().isPresent(),
        "grammarRules", grammarRules,
        "compareFunction", compareFunction
    );
  }

  private String stringCompareFunction(Specification specification) {
    var isCaseSensitive = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.annotation(AsmParserCaseSensitive.class))
        .map(AsmParserCaseSensitive::isCaseSensitive).orElse(false);

    return isCaseSensitive ? "equals" : "equals_insensitive";
  }

  private String grammarRules(Specification specification) {
    var parserCaseSensitive = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.annotation(AsmParserCaseSensitive.class))
        .map(AsmParserCaseSensitive::isCaseSensitive).orElse(false);

    var rules = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.rules().stream())
        .orElse(Stream.empty());

    var codeGenerator = new AssemblyParserCodeGenerator(
        lcbConfiguration().targetName().value().toLowerCase(), parserCaseSensitive, rules
    );

    return codeGenerator.generateRules();
  }
}
