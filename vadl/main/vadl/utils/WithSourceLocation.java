package vadl.utils;

/**
 * Interface representing an entity with a {@link SourceLocation}.
 *
 * <p>Provides a method to retrieve the source location associated with the implementing entity.
 */
public interface WithSourceLocation {
  /**
   * Retrieves the source location associated with the entity.
   *
   * @return the {@link SourceLocation} object that represents the location in the source code.
   */
  SourceLocation sourceLocation();
}
