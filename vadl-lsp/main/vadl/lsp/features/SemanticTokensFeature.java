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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Ast;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.IdentifiableNode;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.Node;
import vadl.ast.nodes.RecursiveAstVisitor;
import vadl.ast.nodes.Statement;
import vadl.lsp.VadlLanguageServer;
import vadl.lsp.document.Document;
import vadl.lsp.document.DocumentSnapshot;
import vadl.utils.SourceLocation;

/**
 * Provides the {@code textDocument/semanticTokens} LSP feature. Should be used by
 * {@link vadl.lsp.VadlTextDocumentService}.
 *
 * <p>This provides semantic tokens for identifiers, which may not be properly detected by our
 * TextMate grammar.
 */
public class SemanticTokensFeature {
  private static final Logger log = LoggerFactory.getLogger(SemanticTokensFeature.class);

  /**
   * Maps Semantic token types to their integer index in the legend (which is part of server
   * capabilities). This is required for encoding semanticTokens responses. Should only contain
   * types that the client supports.
   */
  private final Map<String, Integer> tokenTypesMap = new HashMap<>();

  /**
   * Creates a new instance, configured for a specific client and server's capabilities.
   *
   * @param serverCapabilities as determined by {@link VadlLanguageServer}
   */
  public SemanticTokensFeature(ServerCapabilities serverCapabilities) {
    var tokensProvider = serverCapabilities.getSemanticTokensProvider();
    if (tokensProvider == null) {
      return;
    }

    int index = 0;
    for (String type : tokensProvider.getLegend().getTokenTypes()) {
      this.tokenTypesMap.put(type, index);
      index++;
    }
  }

  /**
   * Provides response to a {@code textDocument/semanticTokens/full} request.
   *
   * @param compilation of the requested document
   * @return LSP response
   */
  public SemanticTokens semanticTokensFull(Document.CompilationInputAndResult compilation) {

    if (compilation.result().ast() == null) {
      log.debug("UNABLE semanticTokens/full: Parser produced no AST for {}",
          compilation.documentSnapshot().path);
      return emptyResult();
    }

    var locations = new Visitor(Objects.requireNonNull(compilation.result().ast()))
        .getIdentifierLocations();
    var tokens = transformToLspTokens(locations, compilation.documentSnapshot());
    var utf16Tokens = compilation.documentSnapshot().calculateUtf16Positions(tokens);
    return new SemanticTokens(utf16Tokens);
  }

  /**
   * Provides empty {@code textDocument/semanticTokens/full} response data. This should be used if
   * something went wrong.
   */
  public static SemanticTokens emptyResult() {
    return new SemanticTokens(List.of());
  }


  private List<Integer> transformToLspTokens(
      List<SourceLocation> locations, DocumentSnapshot snapshot) {

    int tokenType = tokenType2Integer(SemanticTokenTypes.Variable);
    if (tokenType < 0) {
      return List.of();
    }

    List<Integer> lspTokens = new ArrayList<>();
    int previousLine = 1; // SourceLocation line starts at 1
    int previousCol = 1;  // Same for column
    for (var location : locations) {
      int length;
      if (location.end().column() == -1) {
        length = snapshot.getUtf8LineLength(location.end().line() - 1)
            - location.begin().column() + 1;
      } else {
        length = location.end().column() - location.begin().column() + 1;
      }
      if (length <= 0) {
        continue;
      }

      int deltaLine = location.begin().line() - previousLine;
      previousLine = location.begin().line();

      if (deltaLine != 0) {
        previousCol = 1;
      }
      int deltaStart = location.begin().column() - previousCol;
      if (deltaStart <= 0 && deltaLine == 0) {
        // Skip duplicate location
        continue;
      }
      previousCol = location.begin().column();

      // deltaLine, deltaStart, length, tokenType, tokenModifiers
      lspTokens.add(deltaLine);
      lspTokens.add(deltaStart);
      lspTokens.add(length);
      lspTokens.add(tokenType);
      lspTokens.add(0);
    }
    return lspTokens;
  }

  private int tokenType2Integer(String tokenType) {
    return tokenTypesMap.getOrDefault(tokenType, -1);
  }


  private static class Visitor extends RecursiveAstVisitor {
    private final Ast ast;
    private final List<SourceLocation> locations = new ArrayList<>();

    Visitor(Ast ast) {
      this.ast = ast;
    }

    List<SourceLocation> getIdentifierLocations() {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
      // Identifiers within model definitions are visible through model invocations,
      // and thus are out of order
      Collections.sort(locations);
      return locations;
    }

    private void beforeNode(Node node) {
      if (node instanceof IdentifiableNode identifiableNode) {
        identifiableNode.identifier().accept(this);
      }
    }

    @Override
    protected void beforeTravel(Expr expr) {
      beforeNode(expr);
    }

    @Override
    protected void beforeTravel(Statement statement) {
      beforeNode(statement);
    }

    @Override
    protected void beforeTravel(Definition definition) {
      beforeNode(definition);
    }

    @Override
    public Void visit(Identifier identifier) {
      if (identifier.location().isValid()) {
        locations.add(identifier.location());
      }
      return null;
    }

    @Override
    public Void visit(ModelDefinition definition) {
      // Skip ModelDefinition's identifier - it has a specific TextMate style
      // that shouldn't be overridden
      return null;
    }
  }
}
