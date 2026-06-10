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
public class CpuProcessDefinition extends Definition {
  public ProcessKind kind;
  @Child
  public List<Parameter> startupOutputs;
  @Child
  public Statement statement;
  public SourceLocation loc;

  public CpuProcessDefinition(ProcessKind kind, List<Parameter> startupOutputs, Statement stmt,
                       SourceLocation loc) {
    this.kind = kind;
    this.startupOutputs = startupOutputs;
    this.statement = stmt;
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
    if (!startupOutputs.isEmpty()) {
      builder.append(" -> ");
      Parameter.prettyPrintMultiple(indent, startupOutputs, builder);
    }
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
    CpuProcessDefinition that = (CpuProcessDefinition) o;
    return kind == that.kind && Objects.equals(startupOutputs, that.startupOutputs)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, startupOutputs, statement);
  }

  public enum ProcessKind {
    RESET("reset");

    public final String keyword;

    ProcessKind(String keyword) {
      this.keyword = keyword;
    }
  }
}
