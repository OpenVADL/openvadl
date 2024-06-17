package vadl.ast;

import java.util.List;
import java.util.Map;

abstract class SyntaxType {
  abstract boolean isSubTypeOf(SyntaxType other);
}

@SuppressWarnings("checkstyle:methodname")
class CoreType extends SyntaxType {
  private final String name;

  private CoreType(String name) {
    this.name = name;
  }

  static CoreType fromNode(Node node) {
    if (node instanceof Identifier) {
      return CoreType.Id();
    } else if (node instanceof IntegerLiteral) {
      return CoreType.Int();
    } else if (node instanceof BinaryExpr) {
      return CoreType.Bin();
    }
    // FIXME: Add the other cases once implemented in the AST.
    return invalidType;
  }

  private static final CoreType statsType = new CoreType("Stats");
  private static final CoreType statType = new CoreType("Stat");
  private static final CoreType encsType = new CoreType("Encs");
  private static final CoreType isaDefsType = new CoreType("IsaDefs");
  private static final CoreType exType = new CoreType("Ex");
  private static final CoreType litType = new CoreType("Lit");
  private static final CoreType strType = new CoreType("Str");
  private static final CoreType valType = new CoreType("Val");
  private static final CoreType boolType = new CoreType("Bool");
  private static final CoreType intType = new CoreType("Int");
  private static final CoreType binType = new CoreType("Bin");
  private static final CoreType callExType = new CoreType("CallEx");
  private static final CoreType symExType = new CoreType("SymEx");
  private static final CoreType idType = new CoreType("Id");
  private static final CoreType binOpType = new CoreType("BinOp");
  private static final CoreType unOpType = new CoreType("UnOp");
  private static final CoreType invalidType = new CoreType("InvalidType");

  static CoreType Stats() {
    return statsType;
  }

  static CoreType Stat() {
    return statType;
  }

  static CoreType Encs() {
    return encsType;
  }

  static CoreType IsaDefs() {
    return isaDefsType;
  }

  static CoreType Ex() {
    return exType;
  }

  static CoreType Lit() {
    return litType;
  }

  static CoreType Str() {
    return strType;
  }

  static CoreType Val() {
    return valType;
  }

  static CoreType Bool() {
    return boolType;
  }

  static CoreType Int() {
    return intType;
  }

  static CoreType Bin() {
    return binType;
  }

  static CoreType CallEx() {
    return callExType;
  }

  static CoreType SymEx() {
    return symExType;
  }

  static CoreType Id() {
    return idType;
  }

  static CoreType BinOp() {
    return binOpType;
  }

  static CoreType UnOp() {
    return unOpType;
  }

  static CoreType Invalid() {
    return invalidType;
  }

  private final Map<String, List<String>> superTypes = Map.ofEntries(
      Map.entry("Stats", List.of()),
      Map.entry("Stat", List.of("Stats")),
      Map.entry("Encs", List.of()),
      Map.entry("IsaDef", List.of()),
      Map.entry("Ex", List.of()),
      Map.entry("Lit", List.of("Ex")),
      Map.entry("Str", List.of("Ex", "Lit")),
      Map.entry("Val", List.of("Ex", "Lit")),
      Map.entry("Bool", List.of("Ex", "Lit", "Val")),
      Map.entry("Int", List.of("Ex", "Lit", "Val")),
      Map.entry("Bin", List.of("Ex", "Lit", "Val")),
      Map.entry("CallEx", List.of("Ex")),
      Map.entry("SynEx", List.of("Ex", "CallEx")),
      Map.entry("Id", List.of("Ex", "CallEx", "SymEx")),
      Map.entry("BinOp", List.of()),
      Map.entry("UnOp", List.of()),
      Map.entry("InvalidType", List.of())
  );

  @Override
  boolean isSubTypeOf(SyntaxType other) {
    // Each coreType is only once instantiated, so compare references.
    if (this == other) {
      return true;
    }

    if (!(other instanceof CoreType otherCore)) {
      return false;
    }

    var parents = superTypes.get(this.name);
    if (parents == null) {
      throw new RuntimeException("Internal error: could not find supertype " + this.name);
    }
    return parents.contains(otherCore.name);
  }

  @Override
  public String toString() {
    return name;
  }
}
