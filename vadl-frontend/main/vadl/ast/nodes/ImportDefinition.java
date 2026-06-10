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

import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.ast.Ast;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ImportDefinition extends Definition {

  public Ast moduleAst;
  public List<List<Identifier>> importedSymbols;
  @Nullable
  public Identifier fileId;
  @Nullable
  public StringLiteral filePath;
  public List<StringLiteral> args;
  public SourceLocation loc;

  public ImportDefinition(Ast moduleAst, List<List<Identifier>> importedSymbols,
                   @Nullable Identifier fileId, @Nullable StringLiteral filePath,
                   List<StringLiteral> args, SourceLocation loc) {
    requireNonNullElse(fileId, filePath);
    this.moduleAst = moduleAst;
    this.importedSymbols = importedSymbols;
    this.fileId = fileId;
    this.filePath = filePath;
    this.args = args;
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
    builder.append("import ");
    if (fileId != null) {
      fileId.prettyPrint(0, builder);
    } else if (filePath != null) {
      filePath.prettyPrint(0, builder);
    }
    if (!importedSymbols.isEmpty()) {
      builder.append("::{");
      var isFirst = true;
      for (List<Identifier> importedSymbol : importedSymbols) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        var isFirstSegment = true;
        for (Identifier segment : importedSymbol) {
          if (!isFirstSegment) {
            builder.append("::");
          }
          isFirstSegment = false;
          segment.prettyPrint(0, builder);
        }
      }
      builder.append("}");
    }
    if (!args.isEmpty()) {
      builder.append(" with (");
      var isFirst = true;
      for (StringLiteral arg : args) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        arg.prettyPrint(0, builder);
      }
      builder.append(")");
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
    ImportDefinition that = (ImportDefinition) o;
    return Objects.equals(moduleAst, that.moduleAst)
        && Objects.equals(importedSymbols, that.importedSymbols)
        && Objects.equals(fileId, that.fileId)
        && Objects.equals(filePath, that.filePath)
        && Objects.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleAst, importedSymbols, fileId, filePath, args);
  }


}
