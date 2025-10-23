// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.ast;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.lsp4j.SemanticTokenTypes;

/**
 * Tokenizer used by the language server. Provides LSP semantic tokens.
 * Must be in this package, otherwise it can't access {@code Token}.
 */
public final class LspTokenizer {

  private final Map<String, Integer> tokenTypesMap;
  @SuppressWarnings("unused")
  private final Map<String, Integer> tokenModifiersMap;


  /**
   * Creates a new tokenizer.
   *
   * @param tokenTypesMap Maps Semantic token types to their integer index in the legend (which is
   *                      part of server capabilities). This is required for encoding
   *                      semanticTokens responses. Should only contain types that the client
   *                      supports.
   * @param tokenModifiersMap Maps Semantic token modifiers to their integer index in the legend
   *                          (which is part of server capabilities). This is required for encoding
   *                          semanticTokens responses. Should only contain modifiers that the
   *                          client supports.
   */
  public LspTokenizer(Map<String, Integer> tokenTypesMap, Map<String, Integer> tokenModifiersMap) {
    this.tokenTypesMap = tokenTypesMap;
    this.tokenModifiersMap = tokenModifiersMap;
  }

  /**
   * Returns LSP Tokens for the given source code.
   *
   * @param content of a VADL source code file
   * @return Token list encoded for a semanticTokens response
   */
  public List<Integer> getTokens(String content) {
    Scanner scanner = new Scanner(
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
    );

    Token t;
    List<Integer> lspTokens = new ArrayList<>();
    // TODO Handle: transform from utf-8 positions to utf-16 (see LSP specs),
    //              multilineTokenSupport (or lack thereof)
    int previousLine = 1; // Token.line starts at 1
    int previousCol = 1; // Same for Token.col
    while ((t = scanner.Scan()).kind != Parser._EOF) {
      var tokenType = getTokenTypeFromScannerKind(t.kind);
      if (tokenType < 0) {
        continue;
      }

      var deltaLine = t.line - previousLine;
      previousLine = t.line;

      if (deltaLine != 0) {
        previousCol = 1;
      }
      var deltaStart = t.col - previousCol;
      previousCol = t.col;

      // deltaLine, deltaStart, length, tokenType, tokenModifiers
      lspTokens.add(deltaLine);
      lspTokens.add(deltaStart);
      lspTokens.add(t.val.length());
      lspTokens.add(tokenType);
      lspTokens.add(0);
    }
    return lspTokens;
  }

  private int getTokenTypeFromScannerKind(int kind) {
    String type = null;

    // This is where every Scanner token should be mapped to SemanticTokenTypes
    if (ParserUtils.BIN_OPS[kind] || ParserUtils.UN_OPS[kind]) {
      type = SemanticTokenTypes.Operator;
    } else {
      type = switch (kind) {
        case Parser._hexLit, Parser._binLit, Parser._decLit
            -> SemanticTokenTypes.Number;
        // Just a few of the keywords...
        case Parser._ABSOLUTE, Parser._ADDRESS, Parser._ALIAS, Parser._ALIGN, Parser._APPEND,
             Parser._APPLICATION, Parser._ARCHITECTURE, Parser._ASSEMBLY, Parser._BINARY,
             Parser._REGISTER
            -> SemanticTokenTypes.Keyword;
        default -> null;
      };
    }
    // TODO add more

    if (type != null) {
      // Filtering out type that the client doesn't support
      return tokenTypesMap.getOrDefault(type, -1);
    }
    return -1;
  }
}
