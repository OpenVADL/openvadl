package vadl.dump;

import java.util.List;

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

    // fill info with modal title and a copy button
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
