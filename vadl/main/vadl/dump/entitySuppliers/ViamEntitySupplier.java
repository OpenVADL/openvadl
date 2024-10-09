package vadl.dump.entitySuppliers;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import vadl.dump.DumpEntitySupplier;
import vadl.dump.entities.DefinitionEntity;
import vadl.pass.PassResults;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;
import vadl.viam.Specification;

/**
 * A {@link DumpEntitySupplier} that produces a {@link DefinitionEntity}s for each
 * definition in the given VIAM specification.
 */
public class ViamEntitySupplier extends DefinitionVisitor.Empty
    implements DumpEntitySupplier<DefinitionEntity> {

  // all collected entities during the process.
  // it is a LinkedHashMap to preserve the order of entities.
  private final Map<Definition, DefinitionEntity> entities = new LinkedHashMap<>();

  @Override
  public List<DefinitionEntity> getEntities(Specification spec,
                                            PassResults passResults) {
    // triggers the collection process
    callBackVisitor.visit(spec);
    // returns a list of all values
    return entities.values().stream().toList();
  }

  private DefinitionEntity entityOf(Definition definition) {
    var entity = entities.get(definition);
    return Objects.requireNonNull(entity);
  }

  // holds the parents of the currently handled definition
  private final ArrayDeque<DefinitionEntity> parents = new ArrayDeque<>();

  // is run before a definition's children are visited
  private void beforeEach(Definition definition) {
    // create a definition entity from the definition
    var entity = new DefinitionEntity(definition);
    // adds the definition to the entity collection
    entities.put(definition, entity);

    // set parent if available (if Specification it is not available)
    if (!parents.isEmpty()) {
      entity.setParent(parents.peek());
    }
    // push the this definition entity to the parent stack
    parents.push(entity);
  }

  // is run after each child has been processed
  // and this definition was visited (if their was an implementation)
  private void afterEach(@SuppressWarnings("unused") Definition definition) {
    parents.pop();
  }

  // removes the given definition from the entities map and adds it
  // as sub entity to its parent
  private void replaceAsSubEntityOfParent(Definition definition) {
    var entity = entityOf(definition);
    Objects.requireNonNull(entity.parent());
    entity.parent().addSubEntity(null, entity);
    entities.remove(definition);
  }

  /*
  The following visits are called after the children were processed and before
  #afterEach was called.
  It allows to modify entities depending on their needs. Not all definitions are
  implemented by the visitor, as not all of them require special treatment.
   */

  @Override
  public void visit(Specification specification) {
    // we don't emit the specification as it has no valuable information
    // except for the name (which is included in the title).
    entities.remove(specification);
  }


  @Override
  public void visit(vadl.viam.Function function) {
    // Only top-level or ISA level functions should be diplayed in the TOC
    // and at the top-level of the HTML.
    // all other function entities should be added as sub-entities to there parents.

    var entity = entityOf(function);
    Objects.requireNonNull(entity.parent());
    if (!(entity.parent().origin() instanceof Specification)
        && !(entity.parent().origin() instanceof InstructionSetArchitecture)) {
      // remove this from entities and add as sub-entity to parent
      // as it is not a top-level definition
      replaceAsSubEntityOfParent(function);
    }
  }

  @Override
  public void visit(Parameter relocation) {
    // remove it from entities and add it as sub-entity to the parent
    replaceAsSubEntityOfParent(relocation);
  }

  @Override
  public void visit(Format.Field formatField) {
    // remove it from entities and add it as sub-entity to the parent
    replaceAsSubEntityOfParent(formatField);
  }

  @Override
  public void visit(Encoding.Field encodingField) {
    // remove it from entities and add it as sub entity to the parent
    replaceAsSubEntityOfParent(encodingField);
  }


  @Override
  public void visit(Format.FieldAccess fieldAccess) {
    // format fields have well defined sub-entities that can be named.
    // so we compare the sub entities' definitions with the one stored in the
    // field access to determine the name of the sub-entity.
    // this improves readability in the HTML dump.

    var entity = entityOf(fieldAccess);
    for (var se : entity.subEntities()) {
      var de = (DefinitionEntity) se.subEntity;
      if (de.origin() == fieldAccess.encoding()) {
        se.name = "Encoding Function";
      } else if (de.origin() == fieldAccess.accessFunction()) {
        se.name = "Access Function";
      } else if (de.origin() == fieldAccess.predicate()) {
        se.name = "Predicate Function";
      }
    }

    // remove it from entities and add it as sub entity to the parent
    replaceAsSubEntityOfParent(fieldAccess);
  }

  /*
  The following implements the traversal through the VIAM specificaiton.
  The callback visitor first calls #beforeEach, than it recusrively visits the children
  of the definition, than calls this implementation of the visitor and lastly
  calls #afterEach.
  This call back strategy allows to only implement a subset of definitions in this visitor
  and leave the others empty.
   */

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

}
