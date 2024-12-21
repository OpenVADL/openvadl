package vadl.iss.passes.decode.dto;

public interface Renderable {

  /**
   * Render the object to a string. For improved formatting, the context can be used to determine
   * the maximum length of the fields to insert padding.
   *
   * @param context The context to use for rendering
   * @return The rendered string
   */
  String render(RenderContext context);
}
