package vadl.dump.supplier;

import static vadl.dump.supplier.ViamEntitySupplier.DefinitionEntity.cssIdFor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import vadl.dump.DumpEntity;
import vadl.dump.DumpEntitySupplier;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
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
      entity.addInfo(new Info.Tag("Parent", parents.peek().name(), "#" + cssIdFor(definition)));
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

    public static String cssIdFor(Definition def) {
      return def.identifier.name() + "-" + def.getClass().getSimpleName();
    }
  }

}
