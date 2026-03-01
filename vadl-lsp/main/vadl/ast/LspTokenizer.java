// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.List;
import java.util.Map;
import vadl.lsp.DocumentSnapshot;

/**
 * Tokenizer used by the language server. Provides LSP semantic tokens.
 */
public abstract class LspTokenizer {
  private final Map<String, Integer> tokenTypesMap;
  @SuppressWarnings("unused")
  private final Map<String, Integer> tokenModifiersMap;

  /**
   * Creates a new tokenizer.
   *
   * @param tokenTypesMap     Maps Semantic token types to their integer index in the legend (which
   *                          is part of server capabilities). This is required for encoding
   *                          semanticTokens responses. Should only contain types that the client
   *                          supports.
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
   * Provides the legend integer index of the given LSP token type.
   *
   * @param tokenType A String constant of {@link org.eclipse.lsp4j.SemanticTokenTypes}.
   * @return -1 if invalid token type or token type is not configured in the server capabilities.
   */
  protected int getTokenTypeInteger(String tokenType) {
    return tokenTypesMap.getOrDefault(tokenType, -1);
  }

  /**
   * Provides the legend integer index of the given LSP token modifier.
   *
   * @param tokenModifier A String constant of {@link org.eclipse.lsp4j.SemanticTokenModifiers}.
   * @return -1 if invalid token modifer or token modifier is not configured in the server
   *         capabilities.
   */
  protected int getTokenModifierInteger(String tokenModifier) {
    return tokenModifiersMap.getOrDefault(tokenModifier, -1);
  }

  /**
   * Returns LSP Tokens for the given source code.
   *
   * @param snapshot the current state of a VADL source code file
   * @return Token list encoded for a semanticTokens response. Note: deltaStart and length are
   *         calculated for UTF-8 encoding.
   */
  public abstract List<Integer> getTokens(DocumentSnapshot snapshot);
}
