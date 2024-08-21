package vadl.viam.passes.htmlDump;

import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public abstract sealed class Info permits Info.Expandable, Info.Modal, Info.Tag {

  static private final AtomicInteger nextId = new AtomicInteger(0);

  private final int id;

  public Info() {
    id = nextId.getAndIncrement();
  }

  public final int id() {
    return id;
  }


  public final static class Tag extends Info {

    public final String name;
    public final String value;
    @Nullable
    public final String link;

    private Tag(String name, String value, @Nullable String link) {
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
  }

  public final static class Expandable extends Info {
    public String title;
    public String body;
    @Nullable
    public String jsOnFirstOpen;

    public Expandable(String title, String body) {
      this(title, body, null);
    }

    public Expandable(String title, String body, @Nullable String jsOnFirstOpen) {
      this.title = title;
      this.body = body;
      this.jsOnFirstOpen = jsOnFirstOpen;
    }
  }

  public final static class Modal extends Info {
    public String title;
    public String modalTitle;
    public String body;
    @Nullable
    public String jsOnFirstOpen;

    public Modal(String title, String body) {
      this(title, title, body, null);
    }

    public Modal(String title, String modalTitle, String body, @Nullable String jsOnFirstOpen) {
      this.title = title;
      this.modalTitle = modalTitle;
      this.body = body;
      this.jsOnFirstOpen = jsOnFirstOpen;
    }
  }


}
