package vadl.dump;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import vadl.dump.infoEnrichers.ViamEnricherCollection;

/**
 * Represents some information that is attached to some {@link DumpEntity}.
 * Infos are commonly added using {@link InfoEnricher}s registered in the {@link HtmlDumpPass}.
 * There are several variants of {@link Info} with different rendering styles.
 *
 * <p>For simple facts (such as {@code Type}) use the {@link Tag} info, while
 * for information that requires more space to be displayed use {@link Expandable} or
 * {@link Modal}.</p>
 *
 * @see HtmlDumpPass
 * @see InfoEnricher
 * @see ViamEnricherCollection
 */
public abstract sealed class Info permits Info.Expandable, Info.Modal, Info.Tag {

  private static final AtomicInteger nextId = new AtomicInteger(0);

  private final int id;

  public Info() {
    id = nextId.getAndIncrement();
  }

  /**
   * Returns the globally unique identifier of the information.
   * This can be used in the HTML/CSS.
   */
  public final int id() {
    return id;
  }

  public abstract Map<String, Object> renderObj();


  /**
   * Represents a tag information that is attached to a {@link DumpEntity}.
   * Tags are commonly added using {@link InfoEnricher}s registered in the {@link HtmlDumpPass}.
   * Tags are simple facts that consist of a name, a value, and an optional link.
   *
   * @see HtmlDumpPass
   * @see InfoEnricher
   * @see ViamEnricherCollection
   */
  public static final class Tag extends Info {

    public final String name;
    public final String value;
    @Nullable
    public final String link;

    /**
     * Constructs a {@link Tag}.
     *
     * @param name  is the tag name
     * @param value is the tag's value
     * @param link  is the optional link that is available on the value
     */
    public Tag(String name, String value, @Nullable String link) {
      this.name = name;
      this.value = value;
      this.link = link;
    }

    public static Tag of(String name, String value) {
      return new Tag(name, value, null);
    }

    public static Tag of(String name, String value, @Nullable String link) {
      return new Tag(name, value, link);
    }

    @Override
    public Map<String, Object> renderObj() {
      var map = new HashMap<String, Object>();
      map.put("id", id());
      map.put("name", name);
      map.put("value", value);
      if (link != null) {
        map.put("link", link);
      }
      return map;
    }
  }

  /**
   * Represents expandable information that is attached to a {@link DumpEntity}.
   * Expandable information is used to display information that requires more space to be displayed.
   * It consists of a title and a body and can optionally include JavaScript code to be
   * executed when the information is first opened.
   *
   * @see Info
   * @see HtmlDumpPass
   * @see InfoEnricher
   */
  public static final class Expandable extends Info {
    public String title;
    public String body;
    @Nullable
    public String jsOnFirstOpen;


    /**
     * Constructs an {@link Expandable}.
     *
     * @param title         the title (name) displayed on the closed expansion field
     * @param body          the HTML body that is displayed on clicking the expansion
     * @param jsOnFirstOpen the javascript that is executed when the expandable is first opened
     */
    public Expandable(String title, String body, @Nullable String jsOnFirstOpen) {
      this.title = title;
      this.body = body;
      this.jsOnFirstOpen = jsOnFirstOpen;
    }

    public Expandable(String title, String body) {
      this(title, body, null);
    }

    @Override
    public Map<String, Object> renderObj() {
      var map = new HashMap<String, Object>();
      map.put("id", id());
      map.put("title", title);
      map.put("body", body);
      if (jsOnFirstOpen != null) {
        map.put("jsOnFirstOpen", jsOnFirstOpen);
      }
      return map;
    }
  }

  /**
   * Represents modal information that is attached to a {@link DumpEntity}.
   * Modals are used to display information that requires a lot of display space.
   * It consists of a title, a modal title, a body, and can optionally include JavaScript code to be
   * executed when the modal is first opened.
   *
   * <p>An example for such a model can be found at
   * {@link ViamEnricherCollection#BEHAVIOR_SUPPLIER_MODAL}</p>
   *
   * @see Info
   * @see HtmlDumpPass
   * @see InfoEnricher
   */
  public static final class Modal extends Info {
    public String title;
    public String modalTitle;
    public String body;
    @Nullable
    public String jsOnFirstOpen;

    public Modal(String title, String body) {
      this(title, title, body, null);
    }

    /**
     * Constructs a {@link Modal}.
     *
     * @param title         the title (name) displayed on the modal button
     * @param modalTitle    the title (name) displayed in the header of the open modal
     * @param body          the HTML body that is displayed in the body section of the open modal
     * @param jsOnFirstOpen the javascript that is executed when the modal is first opened
     */
    public Modal(String title, String modalTitle, String body, @Nullable String jsOnFirstOpen) {
      this.title = title;
      this.modalTitle = modalTitle;
      this.body = body;
      this.jsOnFirstOpen = jsOnFirstOpen;
    }

    @Override
    public Map<String, Object> renderObj() {
      var map = new HashMap<String, Object>();
      map.put("id", id());
      map.put("title", title);
      map.put("modalTitle", modalTitle);
      map.put("body", body);
      if (jsOnFirstOpen != null) {
        map.put("jsOnFirstOpen", jsOnFirstOpen);
      }
      return map;
    }
  }


}
