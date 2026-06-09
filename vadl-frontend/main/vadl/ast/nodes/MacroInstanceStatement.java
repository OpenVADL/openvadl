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
import vadl.utils.SourceLocation;

/**
 * An internal temporary placeholder of macro instantiations.
 * This node should never leave the parser.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class MacroInstanceStatement extends Statement implements IsMacroInstance {
  public MacroOrPlaceholder macro;
  public List<Node> arguments;
  public SourceLocation loc;

  public MacroInstanceStatement(MacroOrPlaceholder macro, List<Node> arguments,
                                SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return macro.returnType();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
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
    builder.append(")");
  }

  @Override
  public MacroOrPlaceholder macroOrPlaceholder() {
    return macro;
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
