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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.ast.nodes.AsIdExpr;
import vadl.ast.nodes.AsStrExpr;
import vadl.ast.nodes.CallIndexExpr;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.Operator;
import vadl.ast.nodes.PlaceholderDefinition;
import vadl.ast.nodes.PlaceholderExpr;
import vadl.ast.nodes.PlaceholderNode;
import vadl.ast.nodes.PlaceholderStatement;
import vadl.types.BuiltInTable;
import vadl.types.OperationType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;

class AstUtils {

  // FIXME: We decided that in the future this behaivor will be removed and only the
  //  signed/unsigned versions are available.
  // Discussion: https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/287#issuecomment-23771
  // There are some pseudo functions that will get resolved to either the signed or unsigned one.
  private static Map<String, List<String>> pseudoRewrites =
      Map.of("VADL::div", List.of("VADL::sdiv", "VADL::udiv"), "VADL::mod",
          List.of("VADL::smod", "VADL::umod"));

  private static Map<String, BuiltInTable.BuiltIn> nameLookupTable =
      BuiltInTable.builtIns().collect(Collectors.toMap(BuiltInTable.BuiltIn::name, b -> b));

  private static Map<String, String> operatorRewrites = Map.of(
      "&&", "&",
      "||", "|"
  );

  private static Map<String, List<BuiltInTable.BuiltIn>> operatorLookupTable =
      BuiltInTable.builtIns()
          .filter(b -> b.operator() != null)
          .collect(Collectors.groupingBy(BuiltInTable.BuiltIn::operator));


  @Nullable
  static BuiltInTable.BuiltIn getBuiltIn(String name, List<Type> argTypes) {
    if (pseudoRewrites.containsKey(name)) {
      var signed = argTypes.stream().anyMatch(t -> t instanceof SIntType);
      name = pseudoRewrites.get(name).get(signed ? 0 : 1);
    } else if (name.equals("decimal")) {
      // TODO: Remove this once all decimal calls were replaced by s/udec in the VADL specs (#409)
      // set decimal to be an alias of sdec
      name = "sdec";
    }

    return nameLookupTable.get(name);
  }

  static BuiltInTable.BuiltIn getOperatorBuiltIn(Operator operator, List<Type> argTypes) {

    var symbol = operator.symbol;
    if (operatorRewrites.containsKey(symbol)) {
      symbol = operatorRewrites.get(symbol);
    }

    if (operator.equals(Operator.Add) && argTypes.equals(List.of(Type.string(), Type.string()))) {
      return BuiltInTable.CONCATENATE_STRINGS;
    }

    String finalOperatorSymbol = symbol;
    var builtIns = operatorLookupTable.getOrDefault(finalOperatorSymbol, new ArrayList<>());
    builtIns.removeIf(b -> b.signature().argTypeClasses().size() != argTypes.size());

    // Sometimes there are a signed and unsigned version of builtin operation
    return switch (builtIns.size()) {
      case 0 -> throw new IllegalStateException(
          "Couldn't get any matching builtin for %s".formatted(operator));
      case 1 -> builtIns.getFirst();
      case 2 -> {


        final var firstArgType = argTypes.getFirst().getClass();
        if (firstArgType == PseudoFormatType.class) {
          // For opequ/opneq, we select the overload only upon exact match
          builtIns = builtIns.stream()
              .filter(b -> b.signature().argTypeClasses().getFirst() == OperationType.class)
              .toList();
        } else {
          builtIns = builtIns.stream()
              .filter(b -> b.signature().argTypeClasses().getFirst() != OperationType.class)
              .toList();
        }

        if (builtIns.size() == 1) {
          yield builtIns.getFirst();
        }

        final var isSigned = firstArgType == SIntType.class;
        builtIns = builtIns.stream()
            .filter(
                b -> (b.signature().argTypeClasses().getFirst() == SIntType.class) == isSigned)
            .toList();

        if (builtIns.size() != 1) {
          throw new IllegalStateException("Couldn't find a builtin function");
        }

        yield builtIns.getFirst();
      }
      case 3 -> {
        int numSigned = argTypes.get(0).getClass() == SIntType.class ? 1 : 0;
        numSigned += argTypes.get(1).getClass() == SIntType.class ? 1 : 0;

        var targetArgs = switch (numSigned) {
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

  static List<Expr> flatArguments(List<CallIndexExpr.Arguments> argGroups) {
    return argGroups.stream().flatMap(a -> a.values.stream()).collect(Collectors.toList());
  }

  static void forEachArgument(List<CallIndexExpr.Arguments> argGroups, Consumer<Expr> consumer) {
    argGroups.forEach(a -> a.values.forEach(consumer));
  }

  static int argumentCount(List<CallIndexExpr.Arguments> argGroups) {
    int cnt = 0;
    for (var args : argGroups) {
      cnt += args.values.size();
    }
    return cnt;
  }

  static boolean isFullyExpanded(Node node) {
    if (node instanceof ModelDefinition
        || node instanceof PlaceholderNode
        || node instanceof PlaceholderDefinition
        || node instanceof PlaceholderStatement
        || node instanceof PlaceholderExpr
        || node instanceof AsIdExpr
        || node instanceof AsStrExpr) {
      return false;
    }

    AtomicBoolean areChildrenExpanded = new AtomicBoolean(true);
    node.forEachChild(child -> {
      if (!isFullyExpanded(child)) {
        areChildrenExpanded.set(false);
      }
    });
    return areChildrenExpanded.get();

  }
}
