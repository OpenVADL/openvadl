package vadl.dump;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import vadl.pass.PassResults;
import vadl.utils.Pair;

/**
 * Utility functions to create common info objects.
 */
public class InfoUtils {

  /**
   * Constructs a {@link Info.Modal} that renders a DOT graph on first open.
   *
   * @param title      of the modal button
   * @param modalTitle title in the modal header
   * @param dotGraph   graph to be rendered
   */
  public static Info.Modal createGraphModal(String title,
                                            String modalTitle,
                                            String dotGraph) {
    // create new empty modal info and get its id
    var info = new Info.Modal(title, "");
    var id = info.id();

    // fill info with a modal title and a copy button
    info.modalTitle = """
        <div class="flex justify-between items-center w-full">
        <div>%s</div>
        <button type="button" class="text-sm ml-5 rounded-md p-1 border-2 btn btn-secondary" onclick="
            navigator.clipboard.writeText(
                document.getElementById('dot-graph-%s').textContent.trim()
            ).then(() => {
                alert('DOT graph copied to clipboard!');
            });
        ">
            Copy DOT Graph
        </button>
        </div>
        """.formatted(modalTitle, id);

    // add the body with the empty graph container and the dot graph script
    info.body = """
        <div id="graph-%s" class="h-full"></div>
        <script id="dot-graph-%s" type="application/dot">
        %s
        </script>
        """.formatted(id, id, dotGraph);

    // add JavaScript to render the dot graph when the modal is first opened
    info.jsOnFirstOpen = """
        var dotString = document.getElementById('dot-graph-%s').textContent;
        d3.select('#graph-%s')
            .graphviz()
            .width('100%%')
            .height('100%%')
            .renderDot(dotString);
        """.formatted(id, id);

    return info;
  }

  /**
   * Constructs a {@link Info.Modal} that renders a list of DOT graphs.
   * The DOT graph to be shown can be selected using the timeline at the bottom.
   *
   * @param title      of the modal button
   * @param modalTitle title in the modal header
   * @param passGraphs graphs and timeline to be rendered
   */
  @SuppressWarnings("LineLength")
  public static Info.Modal createGraphModalWithTimeline(String title,
                                                        String modalTitle,
                                                        List<Pair<PassResults.SingleResult, String>> passGraphs) {
    // create new empty modal info and get its id
    var info = new Info.Modal(title, "");
    var id = info.id();

    // fill info with a modal title and a copy button
    info.modalTitle = """
        <div class="flex justify-between items-center w-full">
        <div>%s</div>
        <button type="button" class="text-sm ml-5 rounded-md p-1 border-2 btn btn-secondary"
          onclick="copyGraph%s()">
            Copy DOT Graph
        </button>
        </div>
        <script>
          function copyGraph%s() {
            let selectedBtn = document.querySelector('.graph-%s-selected');
            let pass = selectedBtn.id.split('-').splice(2).join('-');
            let id = `dot-graph-%s-${pass}`;
            navigator.clipboard.writeText(
                  document.getElementById(id).textContent.trim()
              ).then(() => {
                  alert('DOT graph copied to clipboard!');
              });
          }
        </script>
        """.formatted(modalTitle, id, id, id, id);

    var passBtns = IntStream.range(0, passGraphs.size())
        .mapToObj(i -> {
          var graphUnSelectedClass =
              i == 0 ? "graph-" + id + "-selected" : "graph-" + id + "-unselected";
          var pass = passGraphs.get(i).left();
          var passId = pass.passKey();
          return """
              <button
                  id="btn-pass-%s"
                  class="bg-blue-100 text-blue-800 p-2 rounded-md flex-shrink-0 %s"
                  onclick="setGraph%s('dot-graph-%s-%s', this)">
                  %s
              </button>
              """.formatted(passId, graphUnSelectedClass, id, id, passId,
              pass.pass().getClass().getSimpleName());
        }).collect(Collectors.joining("\n"));

    var dotGraphScripts = passGraphs.stream().map(p -> """
            <script id="dot-graph-%s-%s" type="application/dot">
              %s
            </script>
            """.formatted(id, p.left().passKey(), p.right()))
        .collect(Collectors.joining("\n"));

    var style = """
        <style>
             .graph-%s-selected {
                 border-color: rgb(30 66 159);
                 border-width: 2px;
             }
             .graph-%s-unselected {
                 border-color: transparent;
                 border-width: 2px;
             }
        </style>
        """.formatted(id, id);

    var setGraphFunc = """
        <script>
            function setGraph%s(id, button) {
                var dotString = document.getElementById(id).textContent;
        
                // Render the graph
                d3.select('#graph-%s')
                    .graphviz()
                    .width('100%%')
                    .height('100%%')
                    .renderDot(dotString);
        
                // Highlight the active button
                document.querySelectorAll('.graph-%s-selected').forEach(btn => {
                    btn.classList.remove('graph-%s-selected');
                    btn.classList.add('graph-%s-unselected');
                });
                button.classList.remove('graph-%s-unselected');
                button.classList.add('graph-%s-selected');
            }
        </script>
        """.formatted(id, id, id, id, id, id, id);

    // add the body with the empty graph container and the dot graph script
    info.body = """
        <div class="flex flex-col h-full">
            <div id="graph-%s" class="flex-grow rounded-md flex items-center justify-center">
                <!-- Graph will render here -->
            </div>
            <div class="flex px-4 pt-4 justify-between">
                <p>< Latest</p>
                <p>Oldest ></p>
            </div>
            <div class="flex p-4 overflow-x-auto space-x-2">
                %s
            </div>
        </div>
        %s
        %s
        %s
        """.formatted(id, passBtns, dotGraphScripts, style, setGraphFunc);

    var firstPass = passGraphs.get(0).left();
    // add JavaScript to render the dot graph when the modal is first opened
    info.jsOnFirstOpen = """
        var initBtn = document.querySelector(
            ".graph-%s-selected"
        );
        setGraph%s("dot-graph-%s-%s", initBtn);
        """.formatted(id, id, id, firstPass.passKey());

    return info;
  }

  /**
   * Constructs a {@link Info.Expandable} that shows a code block.
   *
   * @param title of the modal button (can be HTML)
   * @param code  that is shown in the code block
   */
  public static Info.Expandable createCodeBlockExpandable(String title,
                                                          String code) {
    return new Info.Expandable(
        title,
        """
            <pre><code class="text-sm text-gray-500 whitespace-pre">%s
            </code></pre>
            """.formatted(code)
    );
  }

  /**
   * Constructs a {@link Info.Expandable} that shows a table with custom information.
   *
   * @param title of the modal button (can be HTML)
   * @param table to show. The outer list represents holds the individual columns.
   */
  public static <T> Info.Expandable createTableExpandable(String title,
                                                          List<List<T>> table) {
    StringBuilder html = new StringBuilder(
        "<table class=\"w-full text-sm text-left rtl:text-right text-gray-500 \">");

    // Determine the maximum number of rows
    int maxRows = table.stream().mapToInt(List::size).max().orElse(0);

    // Build each row of the table
    for (int i = 0; i < maxRows; i++) {
      if (i == 0) {
        html.append("<thead class=\"text-xs text-gray-700 uppercase ");
        html.append("bg-gray-50 dark:bg-gray-700 dark:text-gray-400\">");
      }
      html.append("<tr class=\"bg-white border-b\">");
      for (List<T> column : table) {
        // Check if the column has a value for this row index
        if (i < column.size()) {
          html.append("<td class=\"px-6 py-4\">")
              .append(column.get(i))
              .append("</td>");
        } else {
          // Fill empty cells if some columns are shorter
          html.append("<td></td>");
        }
      }
      html.append("</tr>");
      if (i == 0) {
        html.append("</thead>");
      }
    }

    html.append("</table>");

    return new Info.Expandable(title, html.toString());
  }


}
