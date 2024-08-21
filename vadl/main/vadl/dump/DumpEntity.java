package vadl.dump;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract public class DumpEntity {

  private List<DumpEntity> subEntities = new ArrayList<>();
  private List<Info> infos = new ArrayList<Info>();

  public abstract String cssId();

  public abstract TocKey tocKey();

  public abstract String name();

  public DumpEntity addInfo(Info info) {
    infos.add(info);
    return this;
  }

  public DumpEntity addSubEntity(DumpEntity subEntity) {
    subEntities.add(subEntity);
    return this;
  }

  public List<Info.Tag> tagInfos() {
    return infos.stream()
        .filter(Info.Tag.class::isInstance)
        .map(Info.Tag.class::cast)
        .toList();
  }

  public List<Info.Expandable> expandableInfos() {
    return infos.stream()
        .filter(Info.Expandable.class::isInstance)
        .map(Info.Expandable.class::cast)
        .toList();
  }

  public List<Info.Modal> modalInfos() {
    return infos.stream()
        .filter(Info.Modal.class::isInstance)
        .map(Info.Modal.class::cast)
        .toList();
  }

  public List<DumpEntity> subEntities() {
    return subEntities;
  }


  public class TocKey {
    private String name;
    private int rank;

    public TocKey(String name, int rank) {
      this.name = name;
      this.rank = rank;
    }

    public String name() {
      return name;
    }

    public int rank() {
      return rank;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TocKey tocKey = (TocKey) o;
      return rank == tocKey.rank && Objects.equals(name, tocKey.name);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(name);
      result = 31 * result + rank;
      return result;
    }
  }


}
