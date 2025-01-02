package vadl.iss.passes.decode.qemu.dto;

/**
 * Interface for objects that can be rendered to a string (particularly in the context of the QEMU
 * decode tree).
 */
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
