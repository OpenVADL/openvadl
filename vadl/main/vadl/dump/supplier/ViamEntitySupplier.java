package vadl.dump.supplier;

import static vadl.dump.supplier.ViamEntitySupplier.DefinitionEntity.cssIdFor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.dump.DumpEntity;
import vadl.dump.DumpEntitySupplier;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Memory;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.dump.Info;

public class ViamEntitySupplier extends DefinitionVisitor.Empty
    implements DumpEntitySupplier<ViamEntitySupplier.DefinitionEntity> {

  private final Map<Definition, DefinitionEntity> entities = new LinkedHashMap<>();

  @Override
  public List<DefinitionEntity> getEntities(Specification spec,
                                            Map<vadl.pass.PassKey, Object> passResults) {
    callBackVisitor.visit(spec);
    return entities.values().stream().toList();
  }

  private final Stack<Definition> parents = new Stack<>();

  private void beforeEach(Definition definition) {
    var entity = new DefinitionEntity(definition);
    entities.put(definition, entity);

    // set parent
    if (!parents.isEmpty()) {
      entity.parent = parents.peek();
      entity.addInfo(
          new Info.Tag("Parent", parents.peek().identifier.name(), "#" + cssIdFor(definition)));
    }
    parents.push(definition);
  }

  @Override
  public void visit(Specification specification) {
    // do nothing, specification is handled separately
    entities.remove(specification);
  }

  private void afterEach(Definition definition) {
    parents.pop();
  }

  // hidden traversal

  private final ViamEntitySupplier thisRef = this;

  private final DefinitionVisitor.Recursive callBackVisitor = new DefinitionVisitor.Recursive() {
    @Override
    public void beforeTraversal(Definition definition) {
      beforeEach(definition);
      definition.accept(thisRef);
    }

    @Override
    public void afterTraversal(Definition definition) {
      afterEach(definition);
    }
  };

  public static class DefinitionEntity extends DumpEntity {

    @Nullable
    Definition parent;
    Definition origin;

    DefinitionEntity(Definition origin) {
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

    public static String cssIdFor(Definition def) {
      return def.identifier.name() + "-" + def.getClass().getSimpleName();
    }

    private int rank() {
      int rank = 0;
      for (var defCond : defRank) {
        if (defCond.apply(this)) {
          return rank;
        }
        rank++;
      }
      return rank;
    }

    private static List<Function<DefinitionEntity, Boolean>> defRank = List.of(
        is(InstructionSetArchitecture.class),
        is(Resource.class),
        is(Format.class),
        is(Instruction.class),
        isAndISALevel(vadl.viam.Function.class),
        is(Encoding.class),
        is(Format.FieldAccess.class)
    );

    private static Function<DefinitionEntity, Boolean> is(Class<? extends Definition> defClass) {
      return def -> defClass.isInstance(def.origin);
    }

    private static Function<DefinitionEntity, Boolean> isAndISALevel(
        Class<? extends Definition> defClass) {
      return (def) -> defClass.isInstance(def.origin)
          &&
          (def.parent instanceof Specification || def.parent instanceof InstructionSetArchitecture);
    }

  }
}
