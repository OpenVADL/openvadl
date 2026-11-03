// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lsp.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Frontend;
import vadl.lsp.VadlTextDocumentService;
import vadl.lsp.document.Document;
import vadl.utils.SourceLocation;

/**
 * Provides the {@code textDocument/hover} LSP feature. Should be used by
 * {@link vadl.lsp.VadlTextDocumentService}.
 */
public class HoverFeature {
  private static final Logger log = LoggerFactory.getLogger(HoverFeature.class);

  private final List<String> clientContentFormat;

  /**
   * Creates a new instance, configured for a specific client's capabilities.
   *
   * @param params as provided to
   *               {@link org.eclipse.lsp4j.services.LanguageServer#initialize(InitializeParams)}
   */
  public HoverFeature(InitializeParams params) {
    var capabilities = params.getCapabilities().getTextDocument();
    if (capabilities == null || capabilities.getHover() == null
        || capabilities.getHover().getContentFormat() == null) {
      clientContentFormat = List.of();
    } else {
      clientContentFormat = capabilities.getHover().getContentFormat();
    }
  }

  /**
   * Provides response to a {@code textDocument/hover} request.
   *
   * @param params as provided to
   *               {@link org.eclipse.lsp4j.services.TextDocumentService#hover(HoverParams)}
   * @param compilation of the document requested in {@code params}
   * @return LSP response
   */
  public @Nullable Hover hover(HoverParams params, Document.CompilationInputAndResult compilation) {

    if (compilation.result().ast() == null
        || !compilation.result().completedPass().includes(Frontend.AstPass.PARTIALLY_TYPE_CHECKED)
    ) {
      log.debug("UNABLE hover: Parser didn't typecheck AST for {}",
          compilation.documentSnapshot().path);
      return null;
    }

    var position = compilation.documentSnapshot()
        .calculateUtf8Position(params.getPosition(), false);

    // 1) Show type information
    Hover result = typeHover(compilation, position);

    // 2) Show expanded code (for model invocations)
    if (result == null) {
      result = modelExpansionHover(compilation, position);
    }

    return result;
  }

  private @Nullable Hover typeHover(Document.CompilationInputAndResult compilation,
                                    SourceLocation.Position position) {

    var node = AstFinderByPosition.findTypedNode(Objects.requireNonNull(compilation.result().ast()),
        compilation.documentSnapshot().path, position);
    if (node == null) {
      return null;
    }

    return hoverResult(null, node.type().name(),
        compilation.documentSnapshot().calculateUtf16Range(node.location()));
  }

  private @Nullable Hover modelExpansionHover(
      Document.CompilationInputAndResult compilation, SourceLocation.Position position) {

    var nodes = AstFinder.findExpandedNodes(Objects.requireNonNull(compilation.result().ast()),
        compilation.documentSnapshot().path, position);
    if (nodes.isEmpty()) {
      return null;
    }

    var range = compilation.documentSnapshot().calculateUtf16Range(
        nodes.getFirst().location().outermostDirectLocation());
    var prettyPrinted = new ArrayList<String>(nodes.size());
    for (var node : nodes) {
      var builder = new StringBuilder();
      node.prettyPrint(0, builder);
      prettyPrinted.add(builder.toString().trim());
    }
    return hoverResult("This model invocation expands to:",
        String.join("\n", prettyPrinted), range);
  }

  private @Nullable Hover hoverResult(@Nullable String text, @Nullable String sourceCode,
                                      @Nullable Range range) {

    MarkupContent content = null;
    List<String> parts = new ArrayList<>();
    if (text != null) {
      parts.add(text);
    }

    if (clientContentFormat.contains(MarkupKind.MARKDOWN)) {
      if (sourceCode != null) {
        parts.add("```" + VadlTextDocumentService.LANGUAGE_IDENTIFIER + "\n"
            + sourceCode + "\n```");
      }
      content = new MarkupContent(MarkupKind.MARKDOWN, String.join("  \n", parts));

    } else if (clientContentFormat.contains(MarkupKind.PLAINTEXT)) {
      // Fallback
      if (sourceCode != null) {
        parts.add(sourceCode);
      }
      content = new MarkupContent(MarkupKind.PLAINTEXT, String.join("\n", parts));
    }

    if (content == null) {
      return null;
    }

    var result = new Hover(content);
    result.setRange(range);
    return result;
  }
}
