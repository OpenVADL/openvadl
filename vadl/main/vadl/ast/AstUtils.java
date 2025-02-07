package vadl.ast;

import java.util.List;
import java.util.Objects;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;
import vadl.types.UIntType;

class AstUtils {
  static BuiltInTable.BuiltIn getBinOpBuiltIn(BinaryExpr expr) {
    var builtIns = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().size() == 2)
        .filter(b -> Objects.equals(b.operator(), expr.operator().symbol))
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
            .filter(b -> (b.signature().argTypeClasses().equals(targetArgs)))
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
