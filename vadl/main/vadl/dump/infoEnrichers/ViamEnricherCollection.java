package vadl.dump.infoEnrichers;

import static vadl.dump.InfoEnricher.forType;

import java.util.List;
import vadl.dump.InfoUtils;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.dump.entities.DefinitionEntity;
import vadl.viam.DefProp;
import vadl.viam.Instruction;
import vadl.viam.ViamError;

/**
 * A static collection of {@link InfoEnricher} that provides information about
 * VIAM definitions.
 * E.g. the definition type ({@link #DEF_CLASS_SUPPLIER_TAG} and the behavior graph
 * ({@link #BEHAVIOR_SUPPLIER_MODAL}).
 * They are collected in the {@link #all} list and added in the {@link vadl.dump.HtmlDumpPass}.
 */
public class ViamEnricherCollection {

  /**
   * A {@link InfoEnricher} that adds a {@link vadl.dump.Info.Tag} containing the type of
   * the definition.
   * This is only done if the given entity is a
   * {@link DefinitionEntity} and if the origin definition
   * has the prop {@link DefProp.WithType}.
   */
  public static InfoEnricher TYPE_SUPPLIER_TAG = (entity, passResult) -> {
    if (entity instanceof DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithType typed) {
      entity.addInfo(Info.Tag.of("Type", typed.type().toString()));
    }
  };

  /**
   * The DEF_CLASS_SUPPLIER_TAG variable is an instance of the InfoEnricher interface
   * which attaches a {@link Info.Tag} to a given {@link vadl.dump.DumpEntity} based on
   * the information provided by the entity and the result from already executed passes.
   *
   * <p>It is used to enrich {@link DefinitionEntity} objects of type
   * with a tag named "DefType", that represents the simple name of the class of
   * the entity's origin (definition).
   *
   * @see InfoEnricher
   * @see DefinitionEntity
   */
  public static InfoEnricher DEF_CLASS_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (defEntity, passResult) -> {
        var info = Info.Tag.of("DefType", defEntity.origin().getClass().getSimpleName());
        defEntity.addInfo(info);
      });

  /**
   * The PARENT_SUPPLIER_TAG variable is an implementation of the {@link InfoEnricher} interface.
   * It attaches an {@link Info.Tag} object with the "Parent" tag to a given
   * {@link DefinitionEntity} based on the information
   * provided by the entity.
   *
   * @see InfoEnricher
   * @see DefinitionEntity
   */
  public static InfoEnricher PARENT_SUPPLIER_TAG =
      forType(DefinitionEntity.class, (defEntity, passResult) -> {
        var info =
            Info.Tag.of("Parent", defEntity.parent().name(), "#" + defEntity.parent().cssId());
        defEntity.addInfo(info);
      });


  /**
   * BEHAVIOR_SUPPLIER_MODAL is an implementation of the {@link InfoEnricher} interface.
   * It enriches the given {@link vadl.dump.DumpEntity} with information about the
   * behavior of a {@link vadl.viam.Definition} that has a behavior defined.
   *
   * <p>If the entity is an instance of {@link DefinitionEntity} and it
   * has a behavior defined, BEHAVIOR_SUPPLIER_MODAL retrieves the behavior information,
   * creates an {@link Info.Modal} object, and adds it to the entity.
   *
   * <p>While the info has the graph in DOT syntax added in a script tag,
   * the actual rendering happens when the modal is opened the first time.
   * The d3-graphviz js library is used for this part.</p>
   *
   * @see InfoEnricher
   * @see Info
   */
  public static InfoEnricher BEHAVIOR_SUPPLIER_MODAL = (entity, passResult) -> {
    if (entity instanceof DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithBehavior withBehavior) {
      var def = defEntity.origin();
      var behavior = withBehavior.behaviors().get(0);

      // fetch behavior
      var dotGraph = behavior.dotGraph();
      var info = InfoUtils.createGraphModal(
          "Behavior",
          def.name() + " Behavior",
          dotGraph
      );
      defEntity.addInfo(info);
    }
  };


  /**
   * A {@link InfoEnricher} that adds an expandable to definition entities if
   * the verification failed.
   * This helps debugging and finding bugs in the VIAM.
   */
  public static InfoEnricher VERIFY_SUPPLIER_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        try {
          // run verification
          entity.origin().verify();
        } catch (ViamError e) {
          // catch and add error information
          var info = InfoUtils.createCodeBlockExpandable(
              "<span class=\"text-red-500\">Validation Exception</span>",
              e.getMessage()
          );
          entity.addInfo(info);
        }
      });

  /**
   * A {@link InfoEnricher} that adds an expandable to definition entities if
   * the verification failed.
   * This helps debugging and finding bugs in the VIAM.
   */
  public static InfoEnricher SOURCE_CODE_SUPPLIER_EXPANDABLE =
      forType(DefinitionEntity.class, (entity, passResult) -> {
        var sourceLocation = entity.origin().sourceLocation();
        if (entity.origin() instanceof Instruction) {
          sourceLocation = ((Instruction) entity.origin()).behavior().sourceLocation();
        }
        if (!sourceLocation.isValid()) {
          return;
        }
        var source = sourceLocation.toSourceString();
        var info = InfoUtils.createCodeBlockExpandable(
            "Source Code",
            source
        );
        entity.addInfo(info);
      });

  /**
   * A list of all info enrichers for the default VIAM specification.
   */
  public static List<InfoEnricher> all = List.of(
      DEF_CLASS_SUPPLIER_TAG,
      TYPE_SUPPLIER_TAG,
      PARENT_SUPPLIER_TAG,
      BEHAVIOR_SUPPLIER_MODAL,
      VERIFY_SUPPLIER_EXPANDABLE,
      SOURCE_CODE_SUPPLIER_EXPANDABLE
  );

}
