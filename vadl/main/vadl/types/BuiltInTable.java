package vadl.types;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

/**
 * The BuiltInTable class represents a collection of built-in functions and operations in VADL.
 * It provides information about the name, operator, and supported types of each built-in.
 * The methods of the built-ins define the semantics and validation for each built-in.
 */
public class BuiltInTable {

  /**
   * {@code function add ( a : Bits<N>, b : Bits<N> ) -> Bits<N> }
   */
  public static Binary.Add<BitsType, BitsType> ADD =
      new Binary.Add<>(BitsType.class, BitsType.class) {

      };

  /**
   * {@code function adds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status ) }
   */
  public static Binary.Adds<UIntType, UIntType> ADDS_UU =
      new Binary.Adds<>(UIntType.class, UIntType.class) {

      };

  /**
   * {@code function equ ( a : Bits<N>, b : Bits<N> ) -> Bool }
   */
  public static Comparison.Equ<BitsType, BitsType> EQU =
      new Comparison.Equ<>(BitsType.class, BitsType.class) {

      };

  /**
   * The BuiltIn class represents a built-in function or operation in VADL.
   * It provides information about the name, operator, and supported types.
   * Its methods define built-in semantics and validation.
   *
   * <p>This class is abstract and should be extended to define specific built-in functions.</p>
   *
   * @param <R> The return type class of the built-in function
   */
  public static abstract class BuiltIn<R extends Type> {

    public final String name;
    public final @Nullable String operator;
    public final List<Class<? extends Type>> typeClasses;

    private BuiltIn(String name, @Nullable String operator,
                    List<Class<? extends Type>> typeClasses) {
      this.name = name;
      this.operator = operator;
      this.typeClasses = typeClasses;
    }

    public abstract R returnType(List<Type> types);

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
   *
   * @param <A> The first operand type class
   * @param <B> The second operand type class
   * @param <R> The return type class of the binary operation
   */
  public static abstract class Binary<A extends Type, B extends Type, R extends Type>
      extends BuiltIn<R> {
    public final Class<A> firstTypeClass;
    public final Class<B> secondTypeClass;


    @Override
    public final R returnType(List<Type> list) {
      // TODO: Ensure cast before
      //noinspection unchecked
      return returnType((A) list.get(0), (B) list.get(1));
    }

    /**
     * Returns the specific return type for two given operand types.
     *
     * @param first  operand type
     * @param second operand type
     * @return return type
     */
    public abstract R returnType(A first, B second);

    private Binary(String name, @Nullable String operator, Class<A> first, Class<B> second) {
      super(name, operator, List.of(first, second));
      firstTypeClass = first;
      secondTypeClass = second;
    }

    public static abstract class Add<A extends BitsType, B extends BitsType>
        extends Binary<A, B, A> {
      private Add(Class<A> first, Class<B> second) {
        super("ADD", "+", first, second);
      }

      @Override
      public A returnType(A first, B second) {
        return first;
      }
    }

    public static abstract class Adds<A extends BitsType, B extends BitsType>
        extends Binary<A, B, TupleType> {
      private Adds(Class<A> first, Class<B> second) {
        super("ADDS", "+", first, second);
      }

      @Override
      public TupleType returnType(A first, B second) {
        return Type.tuple(first, Type.status());
      }
    }

    public static abstract class Addc<A extends Type, B extends Type>
        extends Binary<A, B, TupleType> {
      private Addc(Class<A> first, Class<B> second) {
        super("ADDC", null, first, second);
      }

      @Override
      public TupleType returnType(A first, B second) {
        return Type.tuple(first, Type.status());
      }
    }

    public static abstract class Satadd<A extends Type, B extends Type> extends Binary<A, B, A> {
      private Satadd(Class<A> first, Class<B> second) {
        super("SATADD", "+", first, second);
      }

      @Override
      public A returnType(A first, B second) {
        return first;
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
   *
   * @param <A> The first operand type class, must extend BitsType
   * @param <B> The second operand type class, must extend BitsType
   */
  public static abstract class Comparison<A extends BitsType, B extends BitsType>
      extends Binary<A, B, BoolType> {

    private Comparison(String name, @Nullable String operator,
                       Class<A> first,
                       Class<B> second) {
      super(name, operator, first, second);
    }

    @Override
    public BoolType returnType(A first, B second) {
      return Type.bool();
    }

    public static abstract class Equ<A extends BitsType, B extends BitsType>
        extends Comparison<A, B> {
      private Equ(Class<A> first, Class<B> second) {
        super("EQU", "=", first, second);
      }
    }

  }


}
