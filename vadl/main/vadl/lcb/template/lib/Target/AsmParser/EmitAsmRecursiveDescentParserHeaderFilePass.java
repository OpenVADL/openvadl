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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.context.CAsmContext;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.types.asmTypes.AsmType;
import vadl.types.asmTypes.GroupAsmType;
import vadl.viam.Specification;
import vadl.viam.asm.elements.AsmAlternative;
import vadl.viam.asm.elements.AsmAlternatives;
import vadl.viam.asm.elements.AsmAssignToAttribute;
import vadl.viam.asm.elements.AsmAssignToLocalVar;
import vadl.viam.asm.elements.AsmFunctionInvocation;
import vadl.viam.asm.elements.AsmGrammarElement;
import vadl.viam.asm.elements.AsmGroup;
import vadl.viam.asm.elements.AsmLocalVarDefinition;
import vadl.viam.asm.elements.AsmLocalVarUse;
import vadl.viam.asm.elements.AsmOption;
import vadl.viam.asm.elements.AsmRepetition;
import vadl.viam.asm.elements.AsmRuleInvocation;
import vadl.viam.asm.elements.AsmStringLiteralUse;
import vadl.viam.asm.rules.AsmBuiltinRule;
import vadl.viam.asm.rules.AsmGrammarRule;
import vadl.viam.asm.rules.AsmNonTerminalRule;
import vadl.viam.asm.rules.AsmTerminalRule;

/**
 * This file includes the definitions for the asm parser.
 */
public class EmitAsmRecursiveDescentParserHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitAsmRecursiveDescentParserHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmRecursiveDescentParser.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().targetName().value()
        + "/AsmParser/AsmRecursiveDescentParser.h";
  }

  record ParsingResultRecord(String type, String functionName, String comment) implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "type", type,
          "functionName", functionName,
          "comment", comment
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var grammarRules = grammarRules(specification);
    var parsedValueStructs = parsedValueStructs(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "parsingResults", grammarRules,
        "parsedValueStructs", parsedValueStructs);
  }

  private List<ParsingResultRecord> grammarRules(Specification specification) {
    return specification.assemblyDescription().map(
        asmDesc -> asmDesc.rules().stream().map(
            rule -> new ParsingResultRecord(
                rule.getAsmType()
                    .toCppTypeString(lcbConfiguration().targetName().value().toLowerCase()),
                rule.simpleName().trim(), rule.simpleName()
            )
        )
    ).orElse(Stream.empty()).toList();
  }

  private List<ParsedValueStruct> parsedValueStructs(
      Specification specification) {
    return specification.assemblyDescription().map(
        asmDesc -> new StructGenerator().getAllStructs(
            lcbConfiguration().targetName().value().toLowerCase(), asmDesc.rules())
    ).orElse(Stream.empty()).toList();
  }
}


class ParsedValueStruct implements Renderable {
  private final String name;
  private final Stream<StructField> fields;

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "name", name,
        "fields", fields.toList()
    );
  }

  record StructField(String name, String cppTypeString) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name", name,
          "cppTypeString", cppTypeString
      );
    }
  }

  public String getName() {
    return name;
  }

  public Stream<StructField> getFields() {
    return fields;
  }

  public ParsedValueStruct(String namespace, String name, Map<String, AsmType> fieldWithAsmType) {
    this.name = name;
    fields = fieldWithAsmType.entrySet().stream().map(
        entry -> new StructField(entry.getKey(), entry.getValue().toCppTypeString(namespace))
    );
  }
}

@DispatchFor(
    value = AsmGrammarElement.class,
    context = CAsmContext.class,
    include = "vadl.viam.asm"
)
class StructGenerator {

  private final CAsmContext ctx;
  private final StringBuilder builder;

  HashSet<ParsedValueStruct> generatedStructs = new HashSet<>();
  private String namespace = "";

  public StructGenerator() {
    this.builder = new StringBuilder();
    this.ctx = new CAsmContext(builder::append, (ctx, element)
        -> StructGeneratorDispatcher.dispatch(this, ctx, element));
  }

  Stream<ParsedValueStruct> getAllStructs(String namespace, List<AsmGrammarRule> rules) {
    this.namespace = namespace;
    rules.forEach(ctx::gen);
    return generatedStructs.stream();
  }

  private void generateStructIfNecessary(AsmType type) {
    if (!(type instanceof GroupAsmType groupType)) {
      return;
    }

    var structName = groupType.toCppTypeString(namespace);
    if (generatedStructs.stream().anyMatch(s -> s.getName().equals(structName))) {
      return;
    }

    var struct =
        new ParsedValueStruct(namespace, groupType.toCppTypeString(namespace),
            groupType.getSubtypeMap());
    generatedStructs.add(struct);
  }

  @Handler
  void handle(CAsmContext ctx, AsmGrammarRule rule) {
    ctx.gen(rule);
  }

  @Handler
  void handle(CAsmContext ctx, AsmNonTerminalRule rule) {
    ctx.gen(rule.getAlternatives());
  }

  @Handler
  void handle(CAsmContext ctx, AsmBuiltinRule rule) {
    // Do nothing because AsmType of builtin rules is never GroupAsmType
  }

  @Handler
  void handle(CAsmContext ctx, AsmTerminalRule rule) {
    // Do nothing because AsmType of terminal rules is never GroupAsmType
  }

  @Handler
  void handle(CAsmContext ctx, AsmAlternative element) {
    element.elements().forEach(ctx::gen);
  }

  @Handler
  void handle(CAsmContext ctx, AsmAlternatives element) {
    generateStructIfNecessary(element.asmType());
    element.alternatives().forEach(ctx::gen);
  }

  @Handler
  void handle(CAsmContext ctx, AsmAssignToAttribute element) {
    // Do nothing because LHS of assignment does not have an AsmType
  }

  @Handler
  void handle(CAsmContext ctx, AsmAssignToLocalVar element) {
    // Do nothing because LHS of assignment does not have an AsmType
  }

  @Handler
  void handle(CAsmContext ctx, AsmFunctionInvocation element) {
    // Do nothing because AsmType of function invocation is never GroupAsmType
  }

  @Handler
  void handle(CAsmContext ctx, AsmGroup element) {
    generateStructIfNecessary(element.asmType());
    ctx.gen(element.alternatives());
  }

  @Handler
  void handle(CAsmContext ctx, AsmLocalVarDefinition element) {
    generateStructIfNecessary(element.asmType());
    if (element.asmLiteral() != null) {
      ctx.gen(element.asmLiteral());
    }
  }

  @Handler
  void handle(CAsmContext ctx, AsmLocalVarUse element) {
    generateStructIfNecessary(element.asmType());
  }

  @Handler
  void handle(CAsmContext ctx, AsmOption element) {
    ctx.gen(element.alternatives());
  }

  @Handler
  void handle(CAsmContext ctx, AsmRepetition element) {
    ctx.gen(element.alternatives());
  }

  @Handler
  void handle(CAsmContext ctx, AsmRuleInvocation element) {
    generateStructIfNecessary(element.asmType());
  }

  @Handler
  void handle(CAsmContext ctx, AsmStringLiteralUse element) {
    // Do nothing because AsmType of string literals is never GroupAsmType
  }
}
