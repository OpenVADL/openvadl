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

import static vadl.lsp.LspUtils.isWithin;
import static vadl.lsp.LspUtils.toUri;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Frontend;
import vadl.ast.nodes.IdentifiableNode;
import vadl.ast.nodes.IsId;
import vadl.lsp.document.Document;

/**
 * Provides the {@code textDocument/definition} LSP feature. Should be used by
 * {@link vadl.lsp.VadlTextDocumentService}.
 */
public class DefinitionFeature {
  private static final Logger log = LoggerFactory.getLogger(DefinitionFeature.class);

  private final boolean clientSupportsDefinitionLink;

  /**
   * Creates a new instance, configured for a specific client's capabilities.
   *
   * @param params as provided to
   *               {@link org.eclipse.lsp4j.services.LanguageServer#initialize(InitializeParams)}
   */
  public DefinitionFeature(InitializeParams params) {
    var capabilities = params.getCapabilities().getTextDocument();
    if (capabilities == null || capabilities.getDefinition() == null
        || capabilities.getDefinition().getLinkSupport() == null) {
      clientSupportsDefinitionLink = false;
    } else {
      clientSupportsDefinitionLink = capabilities.getDefinition().getLinkSupport();
    }
  }

  /**
   * Provides response to a {@code textDocument/definition} request.
   *
   * @param params as provided to
   *        {@link org.eclipse.lsp4j.services.TextDocumentService#definition(DefinitionParams)}
   * @param compilation of the document requested in {@code params}
   * @return LSP response
   */
  public Either<List<? extends Location>, List<? extends LocationLink>>
      definition(DefinitionParams params, Document.CompilationInputAndResult compilation) {

    if (compilation.result().ast() == null
        || !compilation.result().completedPass().includes(Frontend.AstPass.PARTIALLY_NAMES_RESOLVED)
    ) {
      log.debug("UNABLE definition: Parser produced no AST for {}",
          compilation.documentSnapshot().path);
      return emptyResult();
    }

    var position = compilation.documentSnapshot()
        .calculateUtf8Position(params.getPosition(), false);
    IsId identifier = AstFinderByPosition.findIdentifier(
        compilation.result().ast(),
        compilation.documentSnapshot().path,
        position
    );
    if (identifier == null) {
      return emptyResult();
    }
    var target = identifier.target();
    if (target == null || !target.location().isValid()) {
      return emptyResult();
    }
    var targetPath = Objects.requireNonNull(target.location().path());
    var targetDocument = compilation.fileSystemSnapshot()
        .getFileBasedDocumentSnapshot(targetPath);
    if (targetDocument == null) {
      log.debug("Unexpected: Definition target file {} does not exist", targetPath);
      return emptyResult();
    }

    // targetSelectionRange is the location the cursor jumps to, whereas targetRange refers to the
    // whole definition we jump to.
    // See https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#locationLink
    var targetRange = targetDocument.calculateUtf16Range(target.location());
    Range targetSelectionRange = targetRange; // Fallback
    if (target instanceof IdentifiableNode identifiableTarget) {
      targetSelectionRange = targetDocument.calculateUtf16Range(
          identifiableTarget.identifier().location());

      if (!isWithin(targetSelectionRange, targetRange)) {
        // Selection range MUST be contained in target range. If that is not the case, the target
        // identifier is provided by a model invocation, and it is better not to jump anywhere.
        return emptyResult();
      }
    }
    var originSelectionRange = compilation.documentSnapshot()
        .calculateUtf16Range(identifier.location());

    return definitionResult(targetDocument.path, targetRange, targetSelectionRange,
        originSelectionRange);
  }

  private Either<List<? extends Location>, List<? extends LocationLink>> definitionResult(
      Path targetPath, Range targetRange, Range targetSelectionRange, Range originSelectionRange) {

    if (!clientSupportsDefinitionLink) {
      var location = new Location(toUri(targetPath), targetSelectionRange);
      return Either.forLeft(List.of(location));
    }

    var locationLink = new LocationLink(toUri(targetPath), targetRange, targetSelectionRange,
        originSelectionRange);
    return Either.forRight(List.of(locationLink));
  }

  /**
   * Provides empty {@code textDocument/definition} response data. This should be used if something
   * went wrong.
   */
  public static Either<List<? extends Location>, List<? extends LocationLink>> emptyResult() {
    return Either.forLeft(List.of());
  }
}
