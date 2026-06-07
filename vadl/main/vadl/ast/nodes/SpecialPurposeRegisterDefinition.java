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
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class SpecialPurposeRegisterDefinition extends Definition {

  Purpose purpose;
  @Child
  List<ExpandedSequenceCallExpr> exprs;
  SourceLocation loc;

  SpecialPurposeRegisterDefinition(Purpose purpose,
                                   List<ExpandedSequenceCallExpr> sequence,
                                   SourceLocation loc) {
    this.purpose = purpose;
    this.exprs = sequence;
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
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(purpose.keyword);
    builder.append(" = [");
    var joiner = new StringJoiner(", ");
    for (var expr : exprs) {
      StringBuilder tempBuilder = new StringBuilder();
      expr.prettyPrint(indent + 1, tempBuilder);
      joiner.add(tempBuilder.toString());
    }
    builder.append(joiner.toString());
    builder.append("]\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpecialPurposeRegisterDefinition that = (SpecialPurposeRegisterDefinition) o;
    return purpose == that.purpose && Objects.equals(exprs, that.exprs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purpose, exprs);
  }

  enum Purpose {
    RETURN_ADDRESS("return address"),
    RETURN_VALUE("return value"),
    STACK_POINTER("stack pointer"),
    GLOBAL_POINTER("global pointer"),
    FRAME_POINTER("frame pointer"),
    THREAD_POINTER("thread pointer"),
    FUNCTION_ARGUMENT("function argument"),
    CALLER_SAVED("caller saved"),
    CALLEE_SAVED("callee saved");

    private final String keyword;

    Purpose(String keyword) {
      this.keyword = keyword;
    }

    /**
     * Determines how many arguments are allowed.
     * function value = a{0..1} -> ok
     * stack pointer = a{0..1} -> not ok
     */
    public static final Map<Purpose, Occurrence> numberOfExpectedArguments;

    /**
     * Determines how often a definition is allowed in the ABI.
     */
    public static final Map<Purpose, Occurrence> numberOfOccurrencesAbi;

    static {
      numberOfExpectedArguments = Map.of(Purpose.STACK_POINTER, Occurrence.ONE,
          Purpose.RETURN_ADDRESS, Occurrence.ONE,
          Purpose.GLOBAL_POINTER, Occurrence.ONE,
          Purpose.FRAME_POINTER, Occurrence.ONE,
          Purpose.THREAD_POINTER, Occurrence.ONE,
          Purpose.RETURN_VALUE, Occurrence.AT_LEAST_ONE,
          Purpose.CALLER_SAVED, Occurrence.AT_LEAST_ONE,
          Purpose.CALLEE_SAVED, Occurrence.AT_LEAST_ONE,
          Purpose.FUNCTION_ARGUMENT, Occurrence.AT_LEAST_ONE);


      numberOfOccurrencesAbi = Map.of(Purpose.STACK_POINTER, Occurrence.ONE,
          Purpose.RETURN_ADDRESS, Occurrence.ONE,
          Purpose.GLOBAL_POINTER, Occurrence.OPTIONAL,
          Purpose.FRAME_POINTER, Occurrence.ONE,
          Purpose.THREAD_POINTER, Occurrence.OPTIONAL,
          Purpose.RETURN_VALUE, Occurrence.AT_LEAST_ONE,
          Purpose.CALLER_SAVED, Occurrence.ONE,
          Purpose.CALLEE_SAVED, Occurrence.ONE,
          Purpose.FUNCTION_ARGUMENT, Occurrence.ONE);
    }
  }
}
