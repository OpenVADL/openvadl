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

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class RecordInstance extends Node {
  public RecordType type;
  public List<Node> entries;
  public SourceLocation sourceLocation;

  public RecordInstance(RecordType type, List<Node> entries, SourceLocation sourceLocation) {
    this.type = type;
    this.entries = entries;
    this.sourceLocation = sourceLocation;
  }

  @Override
  public SourceLocation location() {
    return sourceLocation;
  }

  @Override
  public SyntaxType syntaxType() {
    return type;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append("(");
    var isFirst = true;
    for (Node entry : entries) {
      if (!isFirst) {
        builder.append(" ; ");
      }
      isFirst = false;
      entry.prettyPrint(0, builder);
    }
    builder.append(")");
  }
}
