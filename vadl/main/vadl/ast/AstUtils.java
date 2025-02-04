package vadl.ast;

import java.util.Objects;
import vadl.types.BuiltInTable;
import vadl.types.SIntType;

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
      default -> throw new IllegalStateException(
          "Too many matching builtin (%d) for %s".formatted(builtIns.size(), expr.operator));
    };
  }
}
