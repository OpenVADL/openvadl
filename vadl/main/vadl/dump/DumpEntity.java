package vadl.dump;

import java.util.ArrayList;
import java.util.List;

abstract public class DumpEntity {

  private List<DumpEntity> subEntities = new ArrayList<>();
  private List<Info> infos = new ArrayList<Info>();

  public abstract String cssId();

  public abstract String tocKey();

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


}
