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

@SuppressWarnings("MissingJavadocType")
public class RecordInstance extends Node {
  RecordType type;
  List<Node> entries;
  SourceLocation sourceLocation;

  RecordInstance(RecordType type, List<Node> entries, SourceLocation sourceLocation) {
    this.type = type;
    this.entries = entries;
    this.sourceLocation = sourceLocation;
  }

  @Override
  public SourceLocation location() {
    return sourceLocation;
  }

  @Override
  SyntaxType syntaxType() {
    return type;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
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
