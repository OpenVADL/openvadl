package vadl.dump.supplier;

import static vadl.dump.supplier.ViamEntitySupplier.DefinitionEntity.cssIdFor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;
import javax.annotation.Nullable;
import vadl.dump.DumpEntity;
import vadl.dump.DumpEntitySupplier;
import vadl.error.VadlError;
import vadl.pass.PassResults;
import vadl.viam.Assembly;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Memory;
import vadl.viam.Parameter;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Relocation;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.dump.Info;

public class ViamEntitySupplier extends DefinitionVisitor.Empty
    implements DumpEntitySupplier<ViamEntitySupplier.DefinitionEntity> {

  private final Map<Definition, DefinitionEntity> entities = new LinkedHashMap<>();

  @Override
  public List<DefinitionEntity> getEntities(Specification spec,
                                            PassResults passResults) {
    callBackVisitor.visit(spec);
    return entities.values().stream().toList();
  }

  private DefinitionEntity entityOf(Definition definition) {
    var entity = entities.get(definition);
    return Objects.requireNonNull(entity);
  }

  private final Stack<DefinitionEntity> parents = new Stack<>();

  private void beforeEach(Definition definition) {
    var entity = new DefinitionEntity(definition);
    entities.put(definition, entity);

    // set parent
    if (!parents.isEmpty()) {
      entity.parent = parents.peek();
    }
    parents.push(entity);
  }

  @Override
  public void visit(Specification specification) {
    // do nothing, specification is handled separately
    entities.remove(specification);
  }

  private void afterEach(Definition definition) {
    parents.pop();
  }

  private void replaceAsSubEntityOfParent(Definition definition) {
    var entity = entityOf(definition);
    Objects.requireNonNull(entity.parent);
    entity.parent.addSubEntity(null, entity);
    entities.remove(definition);
  }

  @Override
  public void visit(vadl.viam.Function function) {
    var entity = entityOf(function);
    Objects.requireNonNull(entity.parent);
    if (entity.parent.origin instanceof Specification
        || entity.parent.origin instanceof InstructionSetArchitecture) {
      // if normal function definitions, do nothing
      return;
    } else {
      // remove this from entities and add as subentity
      replaceAsSubEntityOfParent(function);
    }
  }

  @Override
  public void visit(Parameter relocation) {
    replaceAsSubEntityOfParent(relocation);
  }

  @Override
  public void visit(Format.Field formatField) {
    replaceAsSubEntityOfParent(formatField);
  }

  @Override
  public void visit(Encoding.Field encodingField) {
    replaceAsSubEntityOfParent(encodingField);
  }


  @Override
  public void visit(Format.FieldAccess fieldAccess) {
    var entity = entityOf(fieldAccess);
    for (var se : entity.subEntities()) {
      var de = (DefinitionEntity) se.subEntity;
      if (de.origin == fieldAccess.encoding()) {
        se.name = "Encoding Function";
      } else if (de.origin == fieldAccess.accessFunction()) {
        se.name = "Access Function";
      } else if (de.origin == fieldAccess.predicate()) {
        se.name = "Predicate Function";
      }
    }

    replaceAsSubEntityOfParent(fieldAccess);
  }

  // hidden traversal

  private final ViamEntitySupplier thisRef = this;

  private final DefinitionVisitor.Recursive callBackVisitor = new DefinitionVisitor.Recursive() {
    @Override
    public void beforeTraversal(Definition definition) {
      beforeEach(definition);
    }

    @Override
    public void afterTraversal(Definition definition) {
      definition.accept(thisRef);
      afterEach(definition);
    }
  };

  public static class DefinitionEntity extends DumpEntity {

    @Nullable
    private DefinitionEntity parent;
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

    public DefinitionEntity parent() {
      Objects.requireNonNull(parent);
      return parent;
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
      return (def) -> {
        if (!defClass.isInstance(def.origin)) {
          return false;
        }
        Objects.requireNonNull(def.parent);
        return def.parent.origin instanceof Specification ||
            def.parent.origin instanceof InstructionSetArchitecture;
      };
    }

  }
}
