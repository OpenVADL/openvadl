// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * A statement that defines a new label in an instruction sequence.
 */
public class NewLabelStatement extends InstructionSequenceStatement {

  @Child
  public IdentifierOrPlaceholder labelId;

  public SourceLocation loc;

  public NewLabelStatement(IdentifierOrPlaceholder labelId, SourceLocation loc) {
    this.labelId = labelId;
    this.loc = loc;
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append("newlabel ");
    labelId.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  public Identifier labelId() {
    return (Identifier) labelId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NewLabelStatement that = (NewLabelStatement) o;
    return Objects.equals(labelId, that.labelId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(labelId);
  }
}
