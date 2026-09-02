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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AssemblyDefinition extends Definition {
  public List<IdentifierOrPlaceholder> identifiers;
  public Expr expr;
  public SourceLocation loc;

  // Can hold InstructionDefinition or PseudoInstructionDefinition
  public List<Definition> instructionNodes = new ArrayList<>();

  public AssemblyDefinition(List<IdentifierOrPlaceholder> identifiers, Expr expr,
                     SourceLocation location) {
    this.identifiers = identifiers;
    this.expr = expr;
    this.loc = location;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("assembly ");
    var isFirst = true;
    for (IdentifierOrPlaceholder identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(0, builder);
    }
    builder.append(" = ");
    expr.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(expr);
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
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

    var that = (AssemblyDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifiers, that.identifiers)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifiers);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }
}
