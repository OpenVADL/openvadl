package vadl.types;

import javax.annotation.Nullable;

public class BuiltInTable {

  ///// ARITHMETIC //////

  public final static BuiltIn ADD =
      new BuiltIn("ADD", "+", Type.relation(BitsType.class, BitsType.class, BitsType.class));

  ///// LOGICAL //////

  ///// COMPARISON //////

  ///// SHIFTING //////

  ///// BIT COUNTING //////

  ///// FUNCTIONS //////


  public static class BuiltIn {
    private final String name;
    private final @Nullable String operator;
    protected final RelationType type;

    private BuiltIn(String name, @Nullable String operator,
                    RelationType type) {
      this.name = name;
      this.operator = operator;
      this.type = type;
    }

    private BuiltIn(String name,
                    RelationType type) {
      this(name, null, type);
    }

    public String name() {
      return name;
    }

    @Nullable
    public String operator() {
      return operator;
    }

    /**
     * Determines if the built takes the specified argument type classes.
     *
     * @param args the argument type classes to check
     * @return true if the method takes the specified argument classes, false otherwise
     */
    public final boolean takes(Class<?>... args) {
      if (type.argTypeClass().size() != args.length) {
        return false;
      }

      for (int i = 0; i < args.length; i++) {
        if (type.argTypeClass().get(i) != args[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "VADL::" + name + type;
    }
  }

}
