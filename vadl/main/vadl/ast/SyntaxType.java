package vadl.ast;

import java.util.Map;

abstract class SyntaxType {
  abstract boolean isSubTypeOf(SyntaxType other);
}

@SuppressWarnings("checkstyle:methodname")
class BasicSyntaxType extends SyntaxType {
  private final String name;

  private BasicSyntaxType(String name) {
    this.name = name;
  }

  private static final BasicSyntaxType statsType = new BasicSyntaxType("Stats");
  private static final BasicSyntaxType statType = new BasicSyntaxType("Stat");
  private static final BasicSyntaxType encsType = new BasicSyntaxType("Encs");
  private static final BasicSyntaxType isaDefsType = new BasicSyntaxType("IsaDefs");
  private static final BasicSyntaxType exType = new BasicSyntaxType("Ex");
  private static final BasicSyntaxType litType = new BasicSyntaxType("Lit");
  private static final BasicSyntaxType strType = new BasicSyntaxType("Str");
  private static final BasicSyntaxType valType = new BasicSyntaxType("Val");
  private static final BasicSyntaxType boolType = new BasicSyntaxType("Bool");
  private static final BasicSyntaxType intType = new BasicSyntaxType("Int");
  private static final BasicSyntaxType binType = new BasicSyntaxType("Bin");
  private static final BasicSyntaxType callExType = new BasicSyntaxType("CallEx");
  private static final BasicSyntaxType symExType = new BasicSyntaxType("SymEx");
  private static final BasicSyntaxType idType = new BasicSyntaxType("Id");
  private static final BasicSyntaxType binOpType = new BasicSyntaxType("BinOp");
  private static final BasicSyntaxType unOpType = new BasicSyntaxType("UnOp");
  private static final BasicSyntaxType invalidType = new BasicSyntaxType("InvalidType");

  static BasicSyntaxType Stats() {
    return statsType;
  }

  static BasicSyntaxType Stat() {
    return statType;
  }

  static BasicSyntaxType Encs() {
    return encsType;
  }

  static BasicSyntaxType IsaDefs() {
    return isaDefsType;
  }

  static BasicSyntaxType Ex() {
    return exType;
  }

  static BasicSyntaxType Lit() {
    return litType;
  }

  static BasicSyntaxType Str() {
    return strType;
  }

  static BasicSyntaxType Val() {
    return valType;
  }

  static BasicSyntaxType Bool() {
    return boolType;
  }

  static BasicSyntaxType Int() {
    return intType;
  }

  static BasicSyntaxType Bin() {
    return binType;
  }

  static BasicSyntaxType CallEx() {
    return callExType;
  }

  static BasicSyntaxType SymEx() {
    return symExType;
  }

  static BasicSyntaxType Id() {
    return idType;
  }

  static BasicSyntaxType BinOp() {
    return binOpType;
  }

  static BasicSyntaxType UnOp() {
    return unOpType;
  }

  static BasicSyntaxType Invalid() {
    return invalidType;
  }

  private static final Map<BasicSyntaxType, BasicSyntaxType[]> superTypes = Map.ofEntries(
      Map.entry(statsType, new BasicSyntaxType[] {}),
      Map.entry(statType, new BasicSyntaxType[] {statsType}),
      Map.entry(encsType, new BasicSyntaxType[] {}),
      Map.entry(isaDefsType, new BasicSyntaxType[] {}),
      Map.entry(exType, new BasicSyntaxType[] {}),
      Map.entry(litType, new BasicSyntaxType[] {exType}),
      Map.entry(strType, new BasicSyntaxType[] {exType, litType}),
      Map.entry(valType, new BasicSyntaxType[] {exType, litType}),
      Map.entry(boolType, new BasicSyntaxType[] {exType, litType, valType}),
      Map.entry(intType, new BasicSyntaxType[] {exType, litType, valType}),
      Map.entry(binType, new BasicSyntaxType[] {exType, litType, valType}),
      Map.entry(callExType, new BasicSyntaxType[] {exType}),
      Map.entry(symExType, new BasicSyntaxType[] {exType, callExType}),
      Map.entry(idType, new BasicSyntaxType[] {exType, callExType, symExType}),
      Map.entry(binOpType, new BasicSyntaxType[] {}),
      Map.entry(unOpType, new BasicSyntaxType[] {}),
      Map.entry(invalidType, new BasicSyntaxType[] {})
  );

  @Override
  boolean isSubTypeOf(SyntaxType other) {
    // Each BasicSyntaxType is only once instantiated, so compare references.
    if (this == other) {
      return true;
    }

    if (!(other instanceof BasicSyntaxType otherCore)) {
      return false;
    }

    var parents = superTypes.get(this);
    if (parents == null) {
      throw new RuntimeException("Internal error: could not find supertype " + this.name);
    }

    for (BasicSyntaxType superType : parents) {
      if (superType == otherCore) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return name;
  }
}
