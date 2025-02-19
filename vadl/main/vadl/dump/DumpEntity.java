package vadl.dump;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.dump.entities.DefinitionEntity;

/**
 * The dump entity is an element that is rendered in the HTML dump.
 * Every rectangle in the HTML dump represents one DumpEntity. They are produced by
 * {@link DumpEntitySupplier}, while the most common entity is the
 * {@link DefinitionEntity} provided by the
 * {@link vadl.dump.entitySuppliers.ViamEntitySupplier}, which represents a defninition in the VIAM.
 *
 * <p>Each DumpEntity has a cssId which is a id that is used in the HTML for reference.
 * Additionally each dump entity has a set of {@link Info} and sub-entities.
 * Those sub entities are rendered as boxes within boxes.
 * The name of a DumpEntity is used as title of the box.
 * The {@link TocKey} defines the group/name of the TOC (such as 'Functions') and the
 * rank.</p>
 */
public abstract class DumpEntity {

  private List<Sub> subEntities = new ArrayList<>();
  private List<Info> infos = new ArrayList<Info>();

  public abstract String cssId();

  public abstract TocKey tocKey();

  public abstract String name();

  public DumpEntity addInfo(Info info) {
    infos.add(info);
    return this;
  }

  public DumpEntity addSubEntity(@Nullable String name, DumpEntity subEntity) {
    subEntities.add(new Sub(name, subEntity));
    return this;
  }

  public Map<String, Object> renderObj() {
    var map = new HashMap<String, Object>();
    map.put("name", name());
    map.put("cssId", cssId());
    map.put("tocKey", tocKey().renderObj());
    map.put("subEntities", subEntities.stream().map(Sub::renderObj).toList());
    map.put("tagInfos", tagInfos().stream().map(Info.Tag::renderObj).toList());
    map.put("modalInfos", modalInfos().stream().map(Info.Modal::renderObj).toList());
    map.put("expandableInfos", expandableInfos().stream().map(Info.Expandable::renderObj).toList());
    return map;
  }

  /**
   * Get all tag infos for this entity.
   */
  public List<Info.Tag> tagInfos() {
    return infos.stream()
        .filter(Info.Tag.class::isInstance)
        .map(Info.Tag.class::cast)
        .toList();
  }

  /**
   * Get all expandable infos for this entity.
   */
  public List<Info.Expandable> expandableInfos() {
    return infos.stream()
        .filter(Info.Expandable.class::isInstance)
        .map(Info.Expandable.class::cast)
        .toList();
  }

  /**
   * Get all modal infos for this entity.
   */
  public List<Info.Modal> modalInfos() {
    return infos.stream()
        .filter(Info.Modal.class::isInstance)
        .map(Info.Modal.class::cast)
        .toList();
  }

  public List<Sub> subEntities() {
    return subEntities;
  }


  /**
   * The TocKey groups top-level entities, so they are bundled in the TOC under one
   * group name.
   * Additionally, TocKey has a rank that defines in what position of the TOC the entity
   * group belongs to.
   * The smaller the rank the higher it is visible in the TOC.
   */
  public static class TocKey {
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

    public Map<String, Object> renderObj() {
      var map = new HashMap<String, Object>();
      map.put("name", name);
      map.put("rank", rank);
      return map;
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


  /**
   * The sub entity of a dump entity. It holds the actual entity and a name that
   * is rendered above the nested entity box.
   */
  public static class Sub {
    @Nullable
    public String name;
    public DumpEntity subEntity;

    public Sub(@Nullable String name, DumpEntity subEntity) {
      this.name = name;
      this.subEntity = subEntity;
    }

    public Map<String, Object> renderObj() {
      var map = new HashMap<String, Object>();
      map.put("name", name);
      map.put("subEntity", subEntity.renderObj());
      return map;
    }
  }

}
