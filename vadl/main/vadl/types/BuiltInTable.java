package vadl.types;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The BuiltInTable class represents a collection of built-in functions and operations in VADL.
 * It provides information about the name, operator, and supported types of each built-in.
 * The methods of the built-ins define the semantics and validation for each built-in.
 */
public class BuiltInTable {

  /**
   * The {@code VADL::add} built-in function adds two Bits like operands and
   * returns a UInt or SInt depending on the inputs.
   *
   * <p>{@code function add ( a : Bits<N>, b : Bits<N> ) -> [UInt<N> | SInt<N>] }
   */
  public static Binary.Add ADD =
      new Binary.Add(BitsType.class, BitsType.class) {

      };

  /**
   * The {@code VADL::adds} built-in function adds two UInt values and returns the
   * result together with the status flags.
   *
   * <p>The return type is a tuple of the result and the status.
   *
   * <p>{@code function adds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static Binary.Adds ADDS_UU =
      new Binary.Adds(UIntType.class, UIntType.class) {

      };

  /**
   * The {@code VADL::equ} built-in function compares two values for equality
   * and returns {@code true} if they are equal.
   *
   * <p>It takes two UInt operands and returns a Bool.
   *
   * <p>{@code function equ ( a : UInt<N>, b : UInt<N> ) -> Bool }
   */
  public static Comparison.Equ EQU_UU =
      new Comparison.Equ(UIntType.class, UIntType.class) {

      };

  /**
   * The {@code VADL::equ} built-in function compares two values for equality
   * and returns {@code true} if they are equal.
   *
   * <p>It takes two UInt operands and returns a Bool.
   *
   * <p>{@code function equ ( a : UInt<N>, b : UInt<N> ) -> Bool }
   */
  public static Comparison.Equ EQU_SS =
      new Comparison.Equ(SIntType.class, SIntType.class) {

      };


  private static final List<BuiltIn> builtIns = List.of(
      ADD,
      ADDS_UU,
      EQU_UU,
      EQU_SS
  );

  public static Stream<BuiltIn> builtIns() {
    return builtIns.stream();
  }

  /**
   * The BuiltIn class represents a built-in function or operation in VADL.
   * It provides information about the name, operator, and supported types.
   * Its methods define built-in semantics and validation.
   *
   * <p>This class is abstract and should be extended to define specific built-in functions.</p>
   */
  public abstract static class BuiltIn {

    public final String name;
    public final @Nullable String operator;
    public final List<Class<? extends DataType>> typeClasses;

    private BuiltIn(String name, @Nullable String operator,
                    List<Class<? extends DataType>> typeClasses) {
      this.name = name;
      this.operator = operator;
      this.typeClasses = typeClasses;
    }

    /**
     * Determines if the built takes the specified argument type classes.
     *
     * @param args the argument type classes to check
     * @return true if the method takes the specified argument classes, false otherwise
     */
    public final boolean takes(Class<?>... args) {
      if (typeClasses.size() != args.length) {
        return false;
      }

      for (int i = 0; i < args.length; i++) {
        if (typeClasses.get(i) != args[i]) {
          return false;
        }
      }
      return true;
    }

    /**
     * Computes the concrete return type based on a list of types.
     *
     * @param types the list of input types
     * @return the computed return type
     */
    public abstract DataType returnType(List<DataType> types);

    @Override
    public String toString() {
      return "VADL::" + name + "("
          + typeClasses.stream()
          .map(Class::getSimpleName).collect(
              Collectors.joining(", "))
          + ")";
    }

  }

  /**
   * The Binary class represents a binary operation in VADL.
   * It provides information about the operation name, operator, and supported types.
   * Its methods define the operation's semantics and validation.
   *
   * <p>This class is abstract and should be extended to define specific binary operations.</p>
   */
  public abstract static class Binary
      extends BuiltIn {
    public final Class<? extends DataType> firstTypeClass;
    public final Class<? extends DataType> secondTypeClass;

    @Override
    public final DataType returnType(List<DataType> list) {
      // TODO: Ensure cast before
      //noinspection unchecked
      return returnType(list.get(0), list.get(1));
    }

    /**
     * Returns the specific return type for two given operand types.
     *
     * @param first  operand type
     * @param second operand type
     * @return return type
     */
    public abstract DataType returnType(DataType first, DataType second);

    private Binary(String name, @Nullable String operator, Class<? extends DataType> first,
                   Class<? extends DataType> second) {
      super(name, operator, List.of(first, second));
      firstTypeClass = first;
      secondTypeClass = second;
    }

    /**
     * The Add class represents the binary addition operation in VADL.
     */
    public abstract static class Add extends Binary {
      private Add(Class<? extends DataType> first, Class<? extends DataType> second) {
        super("ADD", "+", first, second);
      }

      @Override
      public DataType returnType(DataType first, DataType second) {
        return first;
      }
    }

    /**
     * The Adds class represents the binary addition operation
     * with additional status information in VADL.
     */
    public abstract static class Adds
        extends Binary {
      private Adds(Class<? extends DataType> first, Class<? extends DataType> second) {
        super("ADDS", "+", first, second);
      }

      @Override
      public TupleType returnType(DataType first, DataType second) {
        return DataType.tuple(first, Type.status());
      }
    }
  }

  /**
   * The Comparison class represents a binary comparison operation in VADL.
   * It extends the Binary class and provides information about the operation name, operator,
   * and supported types.
   * Comparison built-ins will always return a {@link BoolType}.
   *
   * <p>This class is abstract and should be extended to define specific comparison operations.</p>
   */
  public abstract static class Comparison
      extends Binary {

    private Comparison(String name, @Nullable String operator,
                       Class<? extends DataType> first,
                       Class<? extends DataType> second) {
      super(name, operator, first, second);
    }

    @Override
    public BoolType returnType(DataType first, DataType second) {
      return Type.bool();
    }

    /**
     * The Equ class represents the equals to comparison operation in VADL.
     */
    public abstract static class Equ extends Comparison {
      private Equ(Class<? extends DataType> first, Class<? extends DataType> second) {
        super("EQU", "=", first, second);
      }
    }

  }


}
