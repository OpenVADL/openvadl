package vadl.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

/**
 * The top type of VADL's type system.
 * All other types extend it.
 *
 * <p>It provides static methods to retrieve the instances of all
 * concrete types. So to retrieve a type, those methods must be used,
 * such that there is only a single instance per type.</p>
 */
public abstract class Type {

  /**
   * A readable representation of the type.
   *
   * @return the name of the type
   */
  public abstract String name();

  /**
   * Checks if the value with this type can be used as it has the other type.
   * This is only relevant for {@link DataType}s, as they might have the same bit
   * representation.
   * For all other types, it is the same as {@code this == other}
   */
  public boolean isTrivialCastTo(Type other) {
    return other == this;
  }

  public final boolean isData() {
    return this instanceof DataType;
  }

  public final DataType asDataType() {
    ViamError.ensure(isData(), "Not a data type: %s", this);
    return (DataType) this;
  }

  @Override
  public String toString() {
    return name();
  }

  private static final HashMap<Integer, BitsType> bitsTypes = new HashMap<>();

  /**
   * Retrieves the BitsType instance with the specified bit width.
   *
   * @param bitWidth the bit width of the BitsType object
   * @return the BitsType object with the specified bit width
   */
  public static BitsType bits(int bitWidth) {
    return bitsTypes
        .computeIfAbsent(bitWidth, k -> new BitsType(bitWidth));
  }

  private static @Nullable BoolType bool;

  /**
   * Retrieves the instance of the BoolType.
   *
   * @return an instance of BoolType
   */
  public static BoolType bool() {
    if (bool == null) {
      bool = new BoolType();
    }
    return bool;
  }


  private static final HashMap<Integer, SIntType> signedIntTypes = new HashMap<>();

  /**
   * Retrieves the instance of SIntType with the specified bit width.
   *
   * @param bitWidth the bit width of the SIntType object
   * @return the SIntType object with the specified bit width
   */
  public static SIntType signedInt(int bitWidth) {
    return signedIntTypes
        .computeIfAbsent(bitWidth, k -> new SIntType(bitWidth));
  }

  private static final HashMap<Integer, UIntType> unsignedIntTyps = new HashMap<>();

  /**
   * Retrieves the instance of UIntType with the specified bit width.
   *
   * @param bitWidth the bit width of the UIntType object
   * @return the UIntType object with the specified bit width
   */
  public static UIntType unsignedInt(int bitWidth) {
    return unsignedIntTyps
        .computeIfAbsent(bitWidth, k -> new UIntType(bitWidth));
  }

  /**
   * Returns a DummyType object.
   *
   * @return a DummyType object representing a placeholder type
   */
  // TODO: Remove
  public static DummyType dummy() {
    return DummyType.INSTANCE;
  }

  private static @Nullable BitSliceType bitSliceType = null;

  /**
   * Retrieves the instance of BitSliceType.
   *
   * @return the instance of BitSliceType
   */
  public static BitSliceType bitSlice() {
    if (bitSliceType == null) {
      bitSliceType = new BitSliceType();
    }
    return bitSliceType;
  }

  private static final HashMap<Integer, TupleType> tupleTypes = new HashMap<>();

  /**
   * Retrieves the tuple type with the specified subtypes.
   *
   * @param types the subtypes of the tuple
   * @return the tuple type with the specified subtypes
   */
  public static TupleType tuple(Type... types) {
    var hashCode = Arrays.hashCode(types);
    return tupleTypes
        .computeIfAbsent(hashCode, k -> new TupleType(types));
  }

  private static @Nullable StatusType statusType = null;

  /**
   * Retrieves the status type instance.
   *
   * @return the status type instance
   */
  public static StatusType status() {
    if (statusType == null) {
      statusType = new StatusType();
    }
    return statusType;
  }

  private static @Nullable VoidType voidType = null;


  /**
   * Retrieves the instance of VoidType.
   */
  public static VoidType void_() {
    if (voidType == null) {
      voidType = new VoidType();
    }
    return voidType;
  }

  private static @Nullable StringType stringType = null;

  /**
   * Retrieves the instance of StringType.
   */
  public static StringType string() {
    if (stringType == null) {
      stringType = new StringType();
    }
    return stringType;
  }


  private static final HashMap<Integer, RelationType> relationTypes = new HashMap<>();

  /**
   * Retrieves the generic relation type.
   *
   * @param argTypes   the list of argument type classes
   * @param returnType the return type class
   * @return the RelationType instance
   */
  public static RelationType relation(List<Class<? extends Type>> argTypes,
                                      Class<? extends Type> returnType) {
    return relation(argTypes, false, returnType);
  }

  /**
   * Retrieves the generic relation type.
   *
   * @param argTypes   the list of argument type classes
   * @param hasVarArgs the flag indicating if the last argument of kind varargs
   * @param returnType the return type class
   * @return the RelationType instance
   */
  public static RelationType relation(List<Class<? extends Type>> argTypes,
                                      boolean hasVarArgs,
                                      Class<? extends Type> returnType) {
    var hashCode = Objects.hash(argTypes, hasVarArgs, returnType);
    return relationTypes
        .computeIfAbsent(hashCode, k -> new RelationType(argTypes, hasVarArgs, returnType));
  }

  /**
   * Retrieves the generic relation type without arguments.
   *
   * @param returnType the return type class
   * @return the RelationType instance
   */
  public static RelationType relation(Class<? extends Type> returnType) {
    return relation(List.of(), false, returnType);
  }

  /**
   * Retrieves the generic relation type with one argument.
   *
   * @param argType    the argument type class
   * @param returnType the return type class
   * @return the RelationType instance
   */
  public static RelationType relation(Class<? extends Type> argType,
                                      Class<? extends Type> returnType) {
    return relation(List.of(argType), returnType);
  }

  /**
   * Retrieves the generic relation type with two arguments.
   *
   * @param firstArg   the first argument type class
   * @param secondArg  the second argument type class
   * @param returnType the return type class
   * @return the RelationType instance
   */
  public static RelationType relation(Class<? extends Type> firstArg,
                                      Class<? extends Type> secondArg,
                                      Class<? extends Type> returnType) {
    return relation(List.of(firstArg, secondArg), false, returnType);
  }

  private static final HashMap<Integer, ConcreteRelationType> concreteRelationTypes =
      new HashMap<>();

  /**
   * Retrieves the ConcreteRelationType based on the given argument types and return type.
   *
   * @param argTypes   the list of argument types
   * @param returnType the return type
   * @return the ConcreteRelationType instance
   */
  public static ConcreteRelationType concreteRelation(List<Type> argTypes,
                                                      Type returnType) {
    var hashCode = Objects.hash(argTypes, returnType);
    return concreteRelationTypes
        .computeIfAbsent(hashCode, k -> new ConcreteRelationType(argTypes, returnType));
  }

  /**
   * Retrieves a concrete relation type without arguments.
   *
   * @param returnType the return type
   * @return the ConcreteRelationType instance
   */
  public static ConcreteRelationType concreteRelation(Type returnType) {
    return concreteRelation(List.of(), returnType);
  }

  /**
   * Retrieves a single argument concrete relation type.
   *
   * @param argType    the argument type
   * @param returnType the return type
   * @return the ConcreteRelationType instance
   */
  public static ConcreteRelationType concreteRelation(Type argType,
                                                      Type returnType) {
    return concreteRelation(List.of(argType), returnType);
  }

  /**
   * Retrieves the concrete relation type with two arguments.
   *
   * @param firstType  the first argument type
   * @param secondType the second argument type
   * @param returnType the return type
   * @return the ConcreteRelationType instance
   */
  public static ConcreteRelationType concreteRelation(Type firstType,
                                                      Type secondType,
                                                      Type returnType) {
    return concreteRelation(List.of(firstType, secondType), returnType);
  }

  private static final HashMap<Integer, AlternativeType> alternativeTypes = new HashMap<>();

  /**
   * Retrieves the {@link AlternativeType} with the given types.
   */
  public static AlternativeType alternative(Set<Type> types) {
    var hash = Objects.hash(types);
    return alternativeTypes
        .computeIfAbsent(hash, k -> new AlternativeType(types));
  }


  /**
   * Tries to construct a data type with a given bit-width from a given type class.
   *
   * <p>If it is not possible to construct the data type, it will return null.</p>
   */
  @Nullable
  public static <T extends Type> DataType constructDataType(Class<T> typeClass, int bitWidth) {
    if (typeClass == BoolType.class) {
      return bitWidth == 1 ? Type.bool() : null;
    } else if (typeClass == BitsType.class) {
      return Type.bits(bitWidth);
    } else if (typeClass == SIntType.class) {
      return Type.signedInt(bitWidth);
    } else if (typeClass == UIntType.class) {
      return Type.unsignedInt(bitWidth);
    } else {
      return null;
    }
  }


}
