package vadl.dump;

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
                                            String dotGraph
  ) {
    // create new empty modal info and get its id
    var info = new Info.Modal(title, "");
    var id = info.id();

    // fill info with modal title
    info.modalTitle = modalTitle;
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


}
