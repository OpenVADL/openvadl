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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

class AstUtils {

  @Nullable
  static BuiltInTable.BuiltIn getBuiltIn(String name, List<Type> argTypes) {

    // FIXME: We decided that in the future this behaivor will be removed and only the
    //  signed/unsigned versions are available.
    // Discussion: https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/287#issuecomment-23771

    // There are some pseudo functions that will get resolved to either the signed or unsinged one.
    var pseudoRewrites = Map.of("VADL::div", List.of("VADL::sdiv", "VADL::udiv"), "VADL::mod",
        List.of("VADL::smod", "VADL::umod"));
    if (pseudoRewrites.containsKey(name)) {
      var singed = argTypes.stream().anyMatch(t -> t instanceof SIntType);
      name = pseudoRewrites.get(name).get(singed ? 0 : 1);
    }

    String finalBuiltinName = name;
    var matchingBuiltin = BuiltInTable.builtIns()
        .filter(b -> b.name().equals(finalBuiltinName)).toList();

    if (matchingBuiltin.size() > 1) {
      throw new IllegalStateException("Multiple builtin match '$s': " + finalBuiltinName);
    }

    if (matchingBuiltin.isEmpty()) {
      return null;
    }

    return matchingBuiltin.get(0);
  }

  static BuiltInTable.BuiltIn getBinOpBuiltIn(BinaryExpr expr) {

    var operator = expr.operator().symbol;
    var operatorRewrites = Map.of(
        "&&", "&",
        "||", "|"
    );
    if (operatorRewrites.containsKey(operator)) {
      operator = operatorRewrites.get(operator);
    }

    if (expr.operator().equals(Operator.Add) && expr.left.type().equals(Type.string())
        && expr.right.type().equals(Type.string())) {
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


  // FIXME: This is a temporary workaround, a more robust solution should be found in the future
  static UnaryExpr getBuiltinUnOp(CallIndexExpr expr, BuiltInTable.BuiltIn builtin) {
    List<Expr> args =
        !expr.argsIndices.isEmpty() ? expr.argsIndices.get(0).values : new ArrayList<>();
    var operatorSymbol = requireNonNull(builtin.operator());
    if (operatorSymbol.equals("~") && args.get(0).type instanceof BoolType) {
      operatorSymbol = "!";
    }
    var operator = UnaryOperator.fromSymbol(operatorSymbol);
    return
        new UnaryExpr(new UnOp(operator, expr.location), args.get(0));
  }

  // FIXME: This is a temporary workaround, a more robust solution should be found in the future
  static BinaryExpr getBuiltinBinOp(CallIndexExpr expr, BuiltInTable.BuiltIn builtin) {
    var operator = requireNonNull(Operator.fromString(requireNonNull(builtin.operator())));
    return
        new BinaryExpr(expr.argsIndices.get(0).values.get(0),
            new BinOp(operator, expr.location),
            expr.argsIndices.get(0).values.get(1));
  }
}
