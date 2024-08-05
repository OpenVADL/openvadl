package vadl.types;

/**
 * A class that represents the VADL status type.
 *
 * <p>It is actually a tuple of size four with bool elements.
 * These four elements represent status flags:
 * <li>negative</li>
 * <li>zero</li>
 * <li>carry</li>
 * <li>overflow</li>
 * in that order.
 */
public class StatusType extends TupleType {

  protected StatusType() {
    super(Type.bool(), Type.bool(), Type.bool(), Type.bool());
  }
  
  @Override
  public String name() {
    return "Status";
  }

}
