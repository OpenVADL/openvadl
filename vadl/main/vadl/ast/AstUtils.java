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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
    } else if (name.equals("decimal")) {
      // TODO: Remove this once all decimal calls were replaced by s/udec in the VADL specs (#409)
      // set decimal to be an alias of sdec
      name = "sdec";
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

  static BuiltInTable.BuiltIn getOperatorBuiltIn(Operator operator, List<Type> argTypes) {

    var symbol = operator.symbol;
    var operatorRewrites = Map.of(
        "&&", "&",
        "||", "|"
    );
    if (operatorRewrites.containsKey(symbol)) {
      symbol = operatorRewrites.get(symbol);
    }

    if (operator.equals(Operator.Add) && argTypes.equals(List.of(Type.string(), Type.string()))) {
      return BuiltInTable.CONCATENATE_STRINGS;
    }

    String finalOperatorSymbol = symbol;
    var builtIns = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().size() == argTypes.size())
        .filter(b -> Objects.equals(b.operator(), finalOperatorSymbol))
        .toList();

    // Sometimes there are a singed and unsigned version of builtin operation
    return switch (builtIns.size()) {
      case 0 -> throw new IllegalStateException(
          "Couldn't get any matching builtin for %s".formatted(operator));
      case 1 -> builtIns.get(0);
      case 2 -> {
        var isSigned = argTypes.getFirst().getClass() == SIntType.class;
        builtIns = builtIns.stream()
            .filter(b -> (b.signature().argTypeClasses().get(0) == SIntType.class) == isSigned)
            .toList();
        if (builtIns.size() != 1) {
          throw new IllegalStateException("Couldn't find a builtin function");
        }
        yield builtIns.get(0);
      }
      case 3 -> {
        int numSinged = argTypes.get(0).getClass() == SIntType.class ? 1 : 0;
        numSinged += argTypes.get(1).getClass() == SIntType.class ? 1 : 0;

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
              operator,
              builtIns));
    };
  }

  static List<Expr> flatArguments(List<CallIndexExpr.Arguments> args) {
    return args.stream().flatMap(a -> a.values.stream()).collect(Collectors.toList());
  }

}
