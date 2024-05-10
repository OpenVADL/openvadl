package vadl.types;

/**
 * The type of VADL bit-slices.
 *
 * <p>Such slices are used e.g. in VADL formats.
 * {@code rs1 [19..15]}
 *
 * @see vadl.viam.Constant.BitSlice
 */
public class BitSliceType extends Type {

  protected BitSliceType() {
  }

  @Override
  public String name() {
    return "BitSlice";
  }
}
