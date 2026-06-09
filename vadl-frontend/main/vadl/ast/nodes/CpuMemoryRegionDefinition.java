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

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * Represents a memory region in the {@link ProcessorDefinition}.
 * <pre>{@code
 * [ firmware ]
 * [ base: 0x8000000 ]
 * memory region [RAM] DRAM in MEM
 *
 * memory region [ROM] MROM in MEM = {
 *   MEM<4>(0x1000 as Bits<64>) := 0x00000297  // auipc t0, 0x0
 *   MEM<4>(0x1004 as Bits<64>) := 0x02828613  // addi a2, t0, 40
 * }
 * }</pre>
 */
@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class CpuMemoryRegionDefinition extends Definition implements IdentifiableNode {

  public enum MemKind {
    RAM, ROM;

    String keyword() {
      return name();
    }
  }

  public IdentifierOrPlaceholder id;
  public MemKind kind;
  @Child
  public IsId memoryRef;
  @Child
  @Nullable
  public Statement stmt;
  public SourceLocation loc;

  public CpuMemoryRegionDefinition(IdentifierOrPlaceholder id, MemKind kind, IsId memoryRef,
                            @Nullable Statement stmt,
                            SourceLocation loc) {
    this.id = id;
    this.kind = kind;
    this.memoryRef = memoryRef;
    this.stmt = stmt;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  public MemoryDefinition memoryNode() {
    return (MemoryDefinition) requireNonNull(memoryRef.target());
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
    builder.append("memory region [").append(kind.keyword()).append("] ");
    id.prettyPrint(indent, builder);
    builder.append(" in ");
    memoryRef.prettyPrint(indent, builder);
    if (stmt != null) {
      builder.append(" = ");
      stmt.prettyPrint(indent, builder);
    }
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CpuMemoryRegionDefinition that = (CpuMemoryRegionDefinition) o;
    return Objects.equals(id, that.id) && kind == that.kind
        && Objects.equals(memoryRef, that.memoryRef)
        && Objects.equals(stmt, that.stmt);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(memoryRef);
    result = 31 * result + Objects.hashCode(stmt);
    return result;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }
}
