package vadl.types;

/**
 * A type that represents actual data that can be stored in a continues
 * array of bits. All data types have a bit-width in memory.
 *
 * @see BitsType
 * @see BoolType
 * @see TupleType
 */
public abstract class DataType extends Type {

  public abstract int bitWidth();

}
