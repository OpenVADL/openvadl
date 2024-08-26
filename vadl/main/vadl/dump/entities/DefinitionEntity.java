package vadl.dump.entities;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.dump.DumpEntity;
import vadl.dump.entitySuppliers.ViamEntitySupplier;
import vadl.dump.infoEnrichers.ViamEnricherCollection;
import vadl.viam.Definition;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Resource;
import vadl.viam.Specification;

/**
 * A {@link DumpEntity} that represents a VIAM {@link Definition}.
 * It is created by the {@link ViamEntitySupplier} and holds the origin {@link Definition}
 * it represents, as well as the parent {@link DefinitionEntity} of this definition.
 * It also defines the {@link TocKey} for the order of the
 * definition groups in the TOC.
 *
 * <p>Note that the info tag for the parent is created by
 * {@link ViamEnricherCollection#PARENT_SUPPLIER_TAG}.
 */
public class DefinitionEntity extends DumpEntity {

  @Nullable
  private DefinitionEntity parent;
  private Definition origin;

  public DefinitionEntity(Definition origin) {
    this.origin = origin;
  }

  public Definition origin() {
    return origin;
  }

  @Override
  public String cssId() {
    return cssIdFor(origin);
  }

  @Override
  public TocKey tocKey() {
    return new TocKey(origin.getClass().getSimpleName() + "s", rank());
  }

  @Override
  public String name() {
    return origin.identifier.name();
  }

  @SuppressWarnings("NullableProblems")
  public void setParent(DefinitionEntity parent) {
    this.parent = parent;
  }

  public DefinitionEntity parent() {
    Objects.requireNonNull(parent);
    return parent;
  }

  public static String cssIdFor(Definition def) {
    return def.identifier.name() + "-" + def.getClass().getSimpleName();
  }

  // gets the rank of this definition kind
  private int rank() {
    int rank = 0;
    // count from 0 until the rank condition is hit
    for (var defCond : defRank) {
      if (defCond.apply(this)) {
        return rank;
      }
      rank++;
    }
    return rank;
  }


  // the rank conditions that define the TOC rank.
  // the upper definitions are order first.
  private static List<Function<DefinitionEntity, Boolean>> defRank = List.of(
      is(InstructionSetArchitecture.class),
      is(Resource.class),
      is(Format.class),
      is(Instruction.class),
      isAndISALevel(vadl.viam.Function.class),
      is(Encoding.class),
      is(Format.FieldAccess.class)
  );

  // returns true if the definition is an instance of the given class.
  private static Function<DefinitionEntity, Boolean> is(Class<? extends Definition> defClass) {
    return def -> defClass.isInstance(def.origin);
  }

  // returns true if it is of the given class and the declaration level is not more than 1.
  // (0 is top-leve, 1 is ISA level declaration)
  private static Function<DefinitionEntity, Boolean> isAndISALevel(
      Class<? extends Definition> defClass) {
    return (def) -> {
      if (!defClass.isInstance(def.origin)) {
        return false;
      }
      Objects.requireNonNull(def.parent);
      return def.parent.origin instanceof Specification
          || def.parent.origin instanceof InstructionSetArchitecture;
    };
  }

}
