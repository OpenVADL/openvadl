// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.codegen.assembly;

import static vadl.viam.ViamError.ensure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.gcb.passes.assembly.AssemblyRegisterNode;
import vadl.template.Renderable;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.ViamError;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * A helper struct for generating parser code in LLVM.
 */
public class ParserGenerator {
  /**
   * Generates a struct name based on a list of {@link Format.Field}.
   * A format with {@code rd}, {@code mnemonic} and {@code rs1} should
   * generated {@code rdmnemonicrs1}.
   */
  public static String generateStructName(List<Format.Field> fields) {
    return fields.stream()
        .map(ParserGenerator::generateFieldName)
        .collect(Collectors.joining());
  }

  /**
   * Generate a name for the function given a {@link AssemblyConstant}.
   */
  public static String generateConstantName(
      AssemblyConstant assemblyConstant) {
    return assemblyConstant.kind().name().toUpperCase();
  }

  /**
   * Generate a name for the function given a {@link Instruction}.
   */
  public static String generateInstructionName(Instruction instruction) {
    return instruction.identifier.simpleName().trim() + "Instruction";
  }

  /**
   * Generate a field name from a {@link Format.Field}.
   */
  public static String generateFieldName(Format.Field field) {
    return field.identifier.simpleName().trim();
  }

  /**
   * The recursive descent parser has a {@code struct} for every assembly operand combination.
   */
  public record FieldStructEnumeration(String structName, List<String> fieldNames) implements
      Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "structName", structName,
          "fieldNames", fieldNames
      );
    }
  }

  /**
   * Generate the assembly operand's names given a {@link BuiltInCall} and the {@link BuiltIn}
   * is {@link BuiltInTable#CONCATENATE_STRINGS}.
   */
  public static FieldStructEnumeration mapParserRecord(
      BuiltInCall builtin) {
    ensure(builtin.builtIn() == BuiltInTable.CONCATENATE_STRINGS, "node must be concat");
    var names = builtin.arguments().stream()
        .map(ParserGenerator::mapToName)
        .filter(x -> !x.isEmpty())
        .toList();

    return new FieldStructEnumeration(String.join("", names), names);
  }

  /**
   * Get the operand name based on the node.
   */
  public static String mapToName(ExpressionNode x) {
    if (x instanceof BuiltInCall b && b.builtIn() == BuiltInTable.MNEMONIC) {
      return "mnemonic";
    } else if (x instanceof BuiltInCall b && b.builtIn() == BuiltInTable.DECIMAL) {
      if (b.arguments().get(0) instanceof FieldRefNode frn) {
        return mapToName(frn);
      } else if (b.arguments().get(0) instanceof FieldAccessRefNode fac) {
        return mapToName(fac);
      }
    } else if (x instanceof BuiltInCall b && b.builtIn() == BuiltInTable.HEX) {
      if (b.arguments().get(0) instanceof FieldRefNode frn) {
        return mapToName(frn);
      } else if (b.arguments().get(0) instanceof FieldAccessRefNode fac) {
        return mapToName(fac);
      }
    } else if (x instanceof AssemblyRegisterNode ar) {
      return ar.field().formatField().identifier.simpleName();
    } else if (x instanceof AssemblyConstant) {
      return "";
    }

    throw new ViamError("not supported");
  }

  private static String mapToName(FieldRefNode node) {
    return node.formatField().identifier.simpleName();
  }

  private static String mapToName(FieldAccessRefNode node) {
    return node.fieldAccess().identifier.simpleName();
  }

  /**
   * Get the llvm lexer token for a vadl builtin rule.
   *
   * @param ruleName the name of the builtin rule
   * @return the corresponding llvm lexer token
   */
  public static String getLlvmTokenKind(String ruleName) {
    return switch (ruleName) {
      case "IDENTIFIER" -> "AsmToken::Identifier";
      case "STRING" -> "AsmToken::String";
      case "INTEGER" -> "AsmToken::Integer";
      case "EOL" -> "AsmToken::EndOfStatement";
      case "COLON" -> "AsmToken::Colon";
      case "PLUS" -> "AsmToken::Plus";
      case "MINUS" -> "AsmToken::Minus";
      case "TILDE" -> "AsmToken::Tilde";
      case "SLASH" -> "AsmToken::Slash";
      case "BACKSLASH" -> "AsmToken::BackSlash";
      case "LPAREN" -> "AsmToken::LParen";
      case "RPAREN" -> "AsmToken::RParen";
      case "LBRAC" -> "AsmToken::LBrac";
      case "RBRAC" -> "AsmToken::RBrac";
      case "LCURLY" -> "AsmToken::LCurly";
      case "RCURLY" -> "AsmToken::RCurly";
      case "STAR" -> "AsmToken::Star";
      case "DOT" -> "AsmToken::Dot";
      case "COMMA" -> "AsmToken::Comma";
      case "DOLLAR" -> "AsmToken::Dollar";
      case "EQUAL" -> "AsmToken::Equal";
      case "EQUALEQUAL" -> "AsmToken::EqualEqual";
      case "PIPE" -> "AsmToken::Pipe";
      case "PIPEPIPE" -> "AsmToken::PipePipe";
      case "CARET" -> "AsmToken::Caret";
      case "AMP" -> "AsmToken::Amp";
      case "AMPAMP" -> "AsmToken::AmpAmp";
      case "EXCLAIM" -> "AsmToken::Exclaim";
      case "EXCLAIMEQUAL" -> "AsmToken::ExclaimEqual";
      case "PERCENT" -> "AsmToken::Percent";
      case "HASH" -> "AsmToken::Hash";
      case "LESS" -> "AsmToken::Less";
      case "LESSEQUAL" -> "AsmToken::LessEqual";
      case "LESSLESS" -> "AsmToken::LessLess";
      case "LESSGREATER" -> "AsmToken::LessGreater";
      case "GREATER" -> "AsmToken::Greater";
      case "GREATEREQUAL" -> "AsmToken::GreaterEqual";
      case "GREATERGREATER" -> "AsmToken::GreaterGreater";
      case "AT" -> "AsmToken::At";
      case "MINUSGREATER" -> "AsmToken::MinusGreater";
      default -> throw new ViamError("Unknown terminal rule name " + ruleName);
    };
  }
}
