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

package vadl.ast;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

class AstUtils {
  static BuiltInTable.BuiltIn getBinOpBuiltIn(BinaryExpr expr) {

    var operator = expr.operator().symbol;
    var operatorRewrites = Map.of(
        "&&", "&",
        "||", "|"
    );
    if (operatorRewrites.containsKey(operator)) {
      operator = operatorRewrites.get(operator);
    }

    if (expr.operator().equals(Operator.Add) && expr.left.type().equals(Type.string()) &&
        expr.right.type().equals(Type.string())) {
      return BuiltInTable.CONCATENATE_STRINGS;
    }

    String finalOperator = operator;
    var builtIns = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().size() == 2)
        .filter(b -> Objects.equals(b.operator(), finalOperator))
        .toList();

    // Sometimes there are a singed and unsigned version of builtin operation
    return switch (builtIns.size()) {
      case 0 -> throw new IllegalStateException(
          "Couldn't get any matching builtin for %s".formatted(expr.operator));
      case 1 -> builtIns.get(0);
      case 2 -> {
        var singed = Objects.requireNonNull(expr.left.type).getClass() == SIntType.class;
        builtIns = builtIns.stream()
            .filter(b -> (b.signature().argTypeClasses().get(0) == SIntType.class) == singed)
            .toList();
        if (builtIns.size() != 1) {
          throw new IllegalStateException("Couldn't find a builtin function");
        }
        yield builtIns.get(0);
      }
      case 3 -> {
        int numSinged = Objects.requireNonNull(expr.left.type).getClass() == SIntType.class ? 1 : 0;
        numSinged += Objects.requireNonNull(expr.right.type).getClass() == SIntType.class ? 1 : 0;

        var targetArgs = switch (numSinged) {
          case 0 -> List.of(UIntType.class, UIntType.class);
          case 1 -> List.of(SIntType.class, UIntType.class);
          case 2 -> List.of(SIntType.class, SIntType.class);
          default -> throw new IllegalStateException();
        };

        builtIns = builtIns.stream()
            .filter(b -> b.signature().argTypeClasses().equals(targetArgs))
            .toList();
        if (builtIns.size() != 1) {
          throw new IllegalStateException("Couldn't find a builtin function");
        }
        yield builtIns.get(0);

      }
      default -> throw new IllegalStateException(
          "Too many matching builtin (%d) for `%s` found: (%s)".formatted(
              builtIns.size(),
              expr.operator,
              builtIns));
    };
  }
}
