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

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class PatchDefinition extends Definition {
  public Identifier generator;
  public Identifier handle;
  @Nullable
  public IsId reference;
  @Nullable
  public String source;
  public SourceLocation loc;

  public PatchDefinition(Identifier generator, Identifier handle, @Nullable IsId reference,
                  @Nullable String source, SourceLocation loc) {
    this.generator = generator;
    this.handle = handle;
    this.reference = reference;
    this.source = source;
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
    builder.append("patch ");
    generator.prettyPrint(0, builder);
    builder.append(" ");
    handle.prettyPrint(0, builder);
    builder.append(" = ");
    if (reference != null) {
      reference.prettyPrint(0, builder);
    }
    if (source != null) {
      builder.append("-<{").append(source).append("}>-");
    }
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PatchDefinition that = (PatchDefinition) o;
    return Objects.equals(generator, that.generator)
        && Objects.equals(handle, that.handle)
        && Objects.equals(reference, that.reference)
        && Objects.equals(source, that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generator, handle, reference, source);
  }
}
