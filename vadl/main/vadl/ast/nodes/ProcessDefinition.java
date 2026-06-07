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

package vadl.ast;

import java.util.List;
import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class ProcessDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder name;
  @Child
  List<TemplateParam> templateParams;
  @Child
  List<Parameter> inputs;
  @Child
  List<Parameter> outputs;
  @Child
  Statement statement;
  SourceLocation loc;

  ProcessDefinition(IdentifierOrPlaceholder name, List<TemplateParam> templateParams,
                    List<Parameter> inputs, List<Parameter> outputs,
                    Statement statement,
                    SourceLocation loc) {
    this.name = name;
    this.templateParams = templateParams;
    this.inputs = inputs;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("process ");
    name.prettyPrint(indent, builder);
    if (!templateParams.isEmpty()) {
      builder.append("<");
      var isFirst = true;
      for (TemplateParam templateParam : templateParams) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        templateParam.prettyPrint(indent, builder);
      }
      builder.append("> ");
    }
    Parameter.prettyPrintMultiple(indent, inputs, builder);
    if (!outputs.isEmpty()) {
      builder.append(" -> ");
      Parameter.prettyPrintMultiple(indent, outputs, builder);
    }
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProcessDefinition that = (ProcessDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(templateParams, that.templateParams)
        && Objects.equals(inputs, that.inputs)
        && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, templateParams, inputs, outputs, statement);
  }


}
