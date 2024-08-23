package vadl.dump.supplier;

import static vadl.dump.InfoEnricher.forType;

import java.util.List;
import vadl.dump.Info;
import vadl.dump.InfoEnricher;
import vadl.viam.DefProp;
import vadl.viam.ViamError;

/**
 * A static collection of {@link InfoEnricher} that provide information about
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
   * {@link vadl.dump.supplier.ViamEntitySupplier.DefinitionEntity} and if the origin definition
   * has the prop {@link DefProp.WithType}.
   */
  public static InfoEnricher TYPE_SUPPLIER_TAG = (entity, passResult) -> {
    if (entity instanceof ViamEntitySupplier.DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithType typed) {
      entity.addInfo(Info.Tag.of("Type", typed.type().toString()));
    }
  };

  /**
   * The DEF_CLASS_SUPPLIER_TAG variable is an instance of the InfoEnricher interface
   * which attaches a {@link Info.Tag} to a given {@link vadl.dump.DumpEntity} based on
   * the information provided by the entity and the result from already executed passes.
   *
   * <p>It is used to enrich {@link ViamEntitySupplier.DefinitionEntity} objects of type
   * with a tag named "DefType", that represents the simple name of the class of
   * the entity's origin (definition).
   *
   * @see InfoEnricher
   * @see ViamEntitySupplier.DefinitionEntity
   */
  public static InfoEnricher DEF_CLASS_SUPPLIER_TAG =
      forType(ViamEntitySupplier.DefinitionEntity.class, (defEntity, passResult) -> {
        var info = Info.Tag.of("DefType", defEntity.origin.getClass().getSimpleName());
        defEntity.addInfo(info);
      });

  /**
   * The PARENT_SUPPLIER_TAG variable is an implementation of the {@link InfoEnricher} interface.
   * It attaches an {@link Info.Tag} object with the "Parent" tag to a given
   * {@link vadl.dump.supplier.ViamEntitySupplier.DefinitionEntity} based on the information
   * provided by the entity.
   *
   * @see InfoEnricher
   * @see ViamEntitySupplier.DefinitionEntity
   */
  public static InfoEnricher PARENT_SUPPLIER_TAG =
      forType(ViamEntitySupplier.DefinitionEntity.class, (defEntity, passResult) -> {
        var info =
            Info.Tag.of("Parent", defEntity.parent().name(), "#" + defEntity.parent().cssId());
        defEntity.addInfo(info);
      });


  /**
   * BEHAVIOR_SUPPLIER_MODAL is an implementation of the {@link InfoEnricher} interface.
   * It enriches the given {@link vadl.dump.DumpEntity} with information about the
   * behavior of a {@link vadl.viam.Definition} that has a behavior defined.
   *
   * <p>If the entity is an instance of {@link ViamEntitySupplier.DefinitionEntity} and it
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
    if (entity instanceof ViamEntitySupplier.DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithBehavior withBehavior) {
      var def = defEntity.origin();
      var behavior = withBehavior.behaviors().get(0);

      // fetch behavior
      var dotGraph = behavior.dotGraph();

      // create new empty modal info and get its id
      var info = new Info.Modal("Behavior", "");
      var id = info.id();

      // fill info with modal title
      info.modalTitle = def.name() + " Behavior";
      // add the body with the empty graph container
      // and a script tag that contains the dot graph
      info.body = """
          <div id="graph-%s" class="h-full"></div>
          <script id="dot-graph-%s" type="application/dot">
          %s
          </script>
          """.formatted(id, id, dotGraph);
      // add javascript that is executed when the modal is opened the first time.
      // it renders the dot graph and embeds it in the graph container.
      info.jsOnFirstOpen = """
          var dotString =
              document.getElementById(
                  "dot-graph-%s",
              ).textContent;
          d3.select("#graph-%s")
              .graphviz()
              .width("100%%")
              .height("100%%")
              .renderDot(
                  dotString,
              );
          """.formatted(id, id);
      // finally add the info to the entity
      defEntity.addInfo(info);
    }
  };

  public static InfoEnricher VERIFY_SUPPLIER_EXPANDABLE =
      forType(ViamEntitySupplier.DefinitionEntity.class, (entity, passResult) -> {
        try {
          entity.origin.verify();
        } catch (ViamError e) {
          var info = new Info.Expandable(
              """
                  <span class="text-red-500">Validation Exception</span>
                  """,
              """
                  <pre><code id="code-block" class="text-sm text-gray-500 whitespace-pre">%s
                  </code></pre>
                  """.formatted(e.getMessage())
          );
          entity.addInfo(info);
        }
      });

  /**
   * A list of all info enrichers for the default VIAM specification.
   */
  public static List<InfoEnricher> all = List.of(
      DEF_CLASS_SUPPLIER_TAG,
      TYPE_SUPPLIER_TAG,
      PARENT_SUPPLIER_TAG,
      BEHAVIOR_SUPPLIER_MODAL,
      VERIFY_SUPPLIER_EXPANDABLE
  );

}
