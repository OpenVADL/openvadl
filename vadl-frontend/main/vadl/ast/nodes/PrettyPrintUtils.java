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

import java.util.List;
import java.util.Set;

/**
 * A util class to improve pretty printing.
 */
public class PrettyPrintUtils {
  static String indentString(int indent) {
    var indentBy = 2;
    return " ".repeat(indentBy * indent);
  }

  static String indentedNewline(int indent) {
    return "\n" + indentString(indent);
  }

  /// Print multiple nodes seperated by the seperator to the proivded builder
  static <T extends Node> void prettyPrintJoin(String separator, List<T> nodes, int indent,
                                                      StringBuilder builder) {
    for (int i = 0; i < nodes.size(); i++) {
      var node = nodes.get(i);
      if (i > 0) {
        builder.append(separator);
      }
      node.prettyPrint(indent, builder);
    }
  }

  private static boolean insertSpaceBetweenDefs(Definition first, Definition second) {
    // Always insert a space before annotations
    if (!second.annotations.isEmpty()) {
      return true;
    }

    // Group together similar definitions
    var defList = List.of(first.getClass(), second.getClass());
    var tightPackingDefs = Set.of(
        ConstantDefinition.class,
        UsingDefinition.class,
        CounterDefinition.class,
        MemoryDefinition.class,
        AliasDefinition.class,
        RegisterDefinition.class);

    if (tightPackingDefs.containsAll(defList) && first.getClass().equals(second.getClass())) {
      return false;
    }

    // Group together Instruction, Assembly, Encoding
    var instructionDefs =
        Set.of(InstructionDefinition.class, EncodingDefinition.class, AssemblyDefinition.class);
    if (instructionDefs.containsAll(defList)) {
      var firstName = switch (first) {
        case InstructionDefinition instr -> instr.identifier().name;
        case AssemblyDefinition asm ->
            asm.identifiers.size() == 1 ? asm.identifiers.getFirst().pathToString() : null;
        case EncodingDefinition enc -> enc.identifier().name;
        default -> throw new IllegalStateException("Unexpected value: " + first);
      };
      var secondName = switch (second) {
        case InstructionDefinition instr -> instr.identifier().name;
        case AssemblyDefinition asm ->
            asm.identifiers.size() == 1 ? asm.identifiers.getFirst().pathToString() : null;
        case EncodingDefinition enc -> enc.identifier().name;
        default -> throw new IllegalStateException("Unexpected value: " + second);
      };
      if (firstName != null && secondName != null && firstName.equals(secondName)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Pretty print a list of definitions grouping semantic ones together.
   *
   * @param indent of the list.
   * @param builder to write to.
   * @param definitions to print.
   */
  public static void prettyPrintDefinitions(int indent, StringBuilder builder,
                                            List<Definition> definitions) {
    Definition previousDefinition = null;
    for (Definition definition : definitions) {
      if (previousDefinition != null && insertSpaceBetweenDefs(previousDefinition, definition)) {
        builder.append("\n");
      }
      definition.prettyPrint(indent, builder);
      previousDefinition = definition;
    }
  }

  static boolean doesPreferSameline(Node node) {
    return node instanceof BlockStatement;
  }

  static void printWithLinePreference(int indent, Node node, StringBuilder builder ) {
    var onSameLine = doesPreferSameline(node);
    if (!onSameLine) {
      builder.append(indentedNewline(indent + 1));
    } else {
      builder.append(" ");
    }
    node.prettyPrint(indent + (onSameLine ? 1 : 1), builder);
  }
}
