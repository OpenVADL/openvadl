package vadl.types;

import java.util.Objects;
import javax.annotation.Nullable;

public class BuiltInTable {

  // TODO: Move init to specific type.
  public static Binary.Add add = new Binary.Add() {
    @Override
    public Type returnType(TypeList<Type> argTypes) {
      ensureCorrectTypes(argTypes);
      return argTypes.get(0);
    }

    @Override
    public void ensureCorrectTypes(TypeList<Type> argTypes) {
      argTypes.ensureLength(2, "addition must have exactly two arguments");
    }
  };


  public static abstract class BuiltIn {

    public final String name;
    public final @Nullable String operator;


    public BuiltIn(String name, @Nullable String operator) {
      this.name = name;
      this.operator = operator;
    }

    public abstract Type returnType(TypeList<Type> argTypes);

    public abstract void ensureCorrectTypes(TypeList<Type> argTypes);

    @Override
    public String toString() {
      return "VADL::" + name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      BuiltIn builtIn = (BuiltIn) o;
      return Objects.equals(name, builtIn.name) &&
          Objects.equals(operator, builtIn.operator);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(name);
      result = 31 * result + Objects.hashCode(operator);
      return result;
    }
  }

  //  public static abstract class Unary extends BuiltIn {
//
//    public static abstract class
//
//  }
//
  public static abstract class Binary extends BuiltIn {

    public Binary(String name, @Nullable String operator) {
      super(name, operator);
    }

    public static abstract class Add extends Binary {

      public Add() {
        super("ADD", "+");
      }


    }

  }
//
//  public static abstract class Nary extends BuiltIn {
//
//  }


}
