package vadl.template;

import java.util.Map;

/**
 * Marks classes that can be rendered by the template {@link AbstractTemplateRenderingPass}.
 * To produce native images that do not crash at runtime during rendering because of missing
 * reflection information, we reduce the number of allowed variable types to
 * {@code Map, List, String} and primitive types (+ their wrappers).
 * If generators use custom classes for rendering variables, they can implement this interface
 * and the {@link #renderObj()} method to enable it as variable type.
 */
public interface Renderable {

  /**
   * Renders the object into a valid map that can be rendered.
   * The values of the returned map can be one of the supported types and classes
   * that also implement {@link Renderable}.
   */
  Map<String, Object> renderObj();

}
