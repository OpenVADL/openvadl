package vadl.types;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

public class BuiltInTable {

  public static Binary.Add<BitsType, BitsType> ADD =
      new Binary.Add<>(BitsType.class, BitsType.class) {

      };

  /**
   * function adds( a : UInt<N>, b : UInt<N> ) -> ( UInt<N>, Status )
   */
  public static Binary.Adds<UIntType, UIntType> ADD_UU =
      new Binary.Adds<>(UIntType.class, UIntType.class) {

      };

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


}
