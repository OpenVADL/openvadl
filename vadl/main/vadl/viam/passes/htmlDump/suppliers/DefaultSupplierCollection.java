package vadl.viam.passes.htmlDump.suppliers;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.intellij.lang.annotations.Language;
import vadl.viam.DefProp;
import vadl.viam.passes.htmlDump.Info;
import vadl.viam.passes.htmlDump.InfoSupplier;

public class DefaultSupplierCollection {

  public static InfoSupplier TYPE_SUPPLIER = (def, passResult) -> {
    if (def instanceof DefProp.WithType typed) {
      return Info.Tag.of("Type", typed.type().toString());
    }
    return null;
  };

  public static InfoSupplier DEF_CLASS_SUPPLIER =
      (def, passResult) -> Info.Tag.of("DefType", def.getClass().getSimpleName());


  public static InfoSupplier BEHAVIOR_SUPPLIER = (def, passResult) -> {
    if (def instanceof DefProp.WithBehavior withBehavior) {
      var behavior = withBehavior.behaviors().get(0);
      try {
        var dotGraph = behavior.dotGraph();

        var info = new Info.Expandable("Behavior", "");
        var id = info.id();

        info.body = """
            <div id="graph-%s" style="height: 500px"></div>
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
        return info;
      } catch (Exception e) {
        return new Info.Expandable("Behavior", """
            <div>%s<div>
            """.formatted(e.getMessage()));
      }
    } else {
      return null;
    }
  };

  public static InfoSupplier BEHAVIOR_SUPPLIER_MODAL = (def, passResult) -> {
    if (def instanceof DefProp.WithBehavior withBehavior) {
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
        return info;
      } catch (Exception e) {
        return new Info.Expandable("Behavior", """
            <div>%s<div>
            """.formatted(e.getMessage()));
      }
    } else {
      return null;
    }
  };


}
