package vadl.dump.supplier;

import java.util.List;
import vadl.dump.InfoEnricher;
import vadl.viam.DefProp;
import vadl.dump.Info;

public class ViamEnricherCollection {

  public static InfoEnricher TYPE_SUPPLIER = (entity, passResult) -> {
    if (entity instanceof ViamEntitySupplier.DefinitionEntity defEntity
        && defEntity.origin() instanceof DefProp.WithType typed) {
      entity.addInfo(Info.Tag.of("Type", typed.type().toString()));
    }
  };

  public static InfoEnricher DEF_CLASS_SUPPLIER =
      (entity, passResult) -> {
        if (entity instanceof ViamEntitySupplier.DefinitionEntity defEntity) {
          var info = Info.Tag.of("DefType", entity.getClass().getSimpleName());
          defEntity.addInfo(info);
        }
      };


  public static InfoEnricher BEHAVIOR_SUPPLIER_MODAL = (entity, passResult) -> {
    if (entity instanceof ViamEntitySupplier.DefinitionEntity defEntity &&
        defEntity.origin() instanceof DefProp.WithBehavior withBehavior) {
      var def = defEntity.origin();
      var behavior = withBehavior.behaviors().get(0);
      try {
        var dotGraph = behavior.dotGraph();

        var info = new Info.Modal("Behavior", "");
        var id = info.id();

        info.modalTitle = def.name() + " Behavior";
        info.body = """
            <div id="graph-%s" class="h-full"></div>
            <script id="dot-graph-%s" type="application/dot">
            %s
            </script>
            """.formatted(id, id, dotGraph);
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
        defEntity.addInfo(info);
      } catch (Exception e) {
        defEntity.addInfo(
            new Info.Expandable("Behavior", """
                <div>%s<div>
                """.formatted(e.getMessage()))
        );
      }
    }
  };

  public static List<InfoEnricher> all = List.of(
      TYPE_SUPPLIER,
      DEF_CLASS_SUPPLIER,
      BEHAVIOR_SUPPLIER_MODAL
  );

}
