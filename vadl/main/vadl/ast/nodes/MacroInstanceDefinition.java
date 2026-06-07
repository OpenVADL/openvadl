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
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class MacroInstanceDefinition extends Definition implements IsMacroInstance {
  MacroOrPlaceholder macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceDefinition(MacroOrPlaceholder macro, List<Node> arguments,
                                 SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return macro.returnType();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("$");
    if (macro instanceof Macro m) {
      builder.append(m.name().name);
    } else if (macro instanceof MacroPlaceholder mp) {
      builder.append(String.join(".", mp.segments()));
    }
    builder.append("(");
    var isFirst = true;
    for (var arg : arguments) {
      if (!isFirst) {
        builder.append(" ; ");
      }
      isFirst = false;
      arg.prettyPrint(0, builder);
    }
    builder.append(")\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroInstanceDefinition that = (MacroInstanceDefinition) o;
    return macro.equals(that.macro)
        && arguments.equals(that.arguments);
  }

  @Override
  public int hashCode() {
    int result = macro.hashCode();
    result = 31 * result + arguments.hashCode();
    return result;
  }

  @Override
  public MacroOrPlaceholder macroOrPlaceholder() {
    return macro;
  }
}
