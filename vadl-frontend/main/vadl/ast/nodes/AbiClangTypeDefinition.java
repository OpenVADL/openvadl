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
import vadl.utils.SourceLocation;

/**
 * The compiler does not only generate a compiler backend but also a clang frontend.
 * This frontend requires information about the types like: What is the size of an integer?
 * Is it unsigned or signed?
 */
@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AbiClangTypeDefinition extends Definition {
  public TypeName typeName;
  public TypeSize typeSize;
  public SourceLocation loc;

  @Override
  public int hashCode() {
    return Objects.hash(typeName, typeSize);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AbiClangTypeDefinition that = (AbiClangTypeDefinition) o;
    return typeName == that.typeName
        && typeSize == that.typeSize;
  }

  public enum TypeName {
    // Type of the size_t in C.
    SIZE_TYPE("size_t type"),
    INT_MAX_TYPE("int max type");

    public final String keyword;

    TypeName(String keyword) {
      this.keyword = keyword;
    }
  }

  public enum TypeSize {
    UNSIGNED_INT("unsigned int"),
    SIGNED_INT("signed int"),
    UNSIGNED_LONG("unsigned long"),
    SIGNED_LONG("signed  long");

    public final String keyword;

    TypeSize(String keyword) {
      this.keyword = keyword;
    }
  }

  public AbiClangTypeDefinition(TypeName typeName,
                                TypeSize typeSize,
                                SourceLocation loc) {
    this.loc = loc;
    this.typeName = typeName;
    this.typeSize = typeSize;
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
    builder.append(prettyIndentString(indent)).append(typeName.keyword)
        .append(" = ").append(typeSize.keyword);
  }
}
