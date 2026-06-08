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

package vadl.ast.nodes;

import java.util.List;
import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class MacroInstructionDefinition extends Definition {
  public MacroBehaviorKind kind;
  @Child
  public List<Parameter> inputs;
  @Child
  public List<Parameter> outputs;
  @Child
  public Statement statement;
  public SourceLocation loc;

  public MacroInstructionDefinition(MacroBehaviorKind kind, List<Parameter> inputs,
                             List<Parameter> outputs, Statement statement,
                             SourceLocation loc) {
    this.kind = kind;
    this.inputs = inputs;
    this.outputs = outputs;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    Parameter.prettyPrintMultiple(indent, inputs, builder);
    builder.append(" -> ");
    Parameter.prettyPrintMultiple(indent, outputs, builder);
    builder.append(" =\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MacroInstructionDefinition that = (MacroInstructionDefinition) o;
    return kind == that.kind && Objects.equals(inputs, that.inputs)
        && Objects.equals(outputs, that.outputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, inputs, outputs, statement);
  }

  public enum MacroBehaviorKind {
    TRANSLATION("translation"), PREDICTION("prediction"), FETCH("fetch"), DECODER("decoder"),
    STARTUP("startup");

    private final String keyword;

    MacroBehaviorKind(String keyword) {
      this.keyword = keyword;
    }
  }
}
