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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openvadl.klsp.protocol.SemanticTokenTypes;

/**
 * Tokenizer used by the language server. Provides LSP semantic tokens.
 * Must be in this package, otherwise it couldn't access {@code Token}.
 */
public final class LspTokenizer {

  private final Map<String, Integer> tokenTypesMap;
  @SuppressWarnings("unused")
  private final Map<String, Integer> tokenModifiersMap;

  /**
   * Maps VADL Scanner Token Kinds to LSP token types.
   */
  private static final String[] tokenKindsMap;

  static {
    // Generate tokenKindsMap
    tokenKindsMap = new String[Parser.maxT + 1];

    // Hardcoded mappings:
    tokenKindsMap[Parser._hexLit] = SemanticTokenTypes.NUMBER;
    tokenKindsMap[Parser._binLit] = SemanticTokenTypes.NUMBER;
    tokenKindsMap[Parser._decLit] = SemanticTokenTypes.NUMBER;
    tokenKindsMap[Parser._identifierToken] = SemanticTokenTypes.VARIABLE;
    tokenKindsMap[Parser._string] = SemanticTokenTypes.STRING;

    // Look through all known token kinds
    for (Field field : Parser.class.getDeclaredFields()) {
      var m = field.getModifiers();
      if (!Modifier.isPublic(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)
          || !int.class.isAssignableFrom(field.getType())) {
        continue;
      }
      var name = field.getName();
      if (!name.startsWith("_")) {
        continue;
      }
      int kind;
      try {
        kind = field.getInt(null);
      } catch (IllegalAccessException e) {
        continue;
      }

      if (tokenKindsMap[kind] != null) {
        // This mapping has already been set above
        continue;
      }
      // Operators according to ParserUtils / Token name "SYM_*"
      if (ParserUtils.BIN_OPS[kind] || ParserUtils.UN_OPS[kind] || name.startsWith("_SYM_")) {
        tokenKindsMap[kind] = SemanticTokenTypes.OPERATOR;
        continue;
      }
      // Token name "T_*"
      if (name.startsWith("_T_")) {
        // Don't know what to map these to
        continue;
      }
      // Everything else should be Keyword
      tokenKindsMap[kind] = SemanticTokenTypes.KEYWORD;
    }
  }


  /**
   * Creates a new tokenizer.
   *
   * @param tokenTypesMap     Maps Semantic token types to their integer index in the legend (which is
   *                          part of server capabilities). This is required for encoding
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
   * Returns LSP Tokens for the given source code.
   *
   * @param content of a VADL source code file
   * @return Token list encoded for a semanticTokens response. Note: deltaStart and length are
   *     calculated for UTF-8 encoding.
   */
  public List<Integer> getTokens(String content) {
    Scanner scanner = new Scanner(
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
    );

    // Note: We assume that all Tokens are single-line, i.e. no need to split them up at line
    //       boundaries for LSP. So far, the only elements in OpenVADL that may span multiple lines
    //       are comments, but the CocoR Scanner doesn't produce Tokens for those anyway.

    List<Integer> lspTokens = new ArrayList<>();
    int previousLine = 1; // Token.line starts at 1
    int previousCol = 1; // Same for Token.col
    for (Token t = scanner.Scan(); t.kind != Parser._EOF; t = scanner.Scan()) {
      int tokenType = getTokenTypeFromScannerKind(t.kind);
      if (tokenType < 0) {
        continue;
      }

      int deltaLine = t.line - previousLine;
      previousLine = t.line;

      if (deltaLine != 0) {
        previousCol = 1;
      }
      int deltaStart = t.col - previousCol;
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
    return tokenTypesMap.getOrDefault(tokenKindsMap[kind], -1);
  }
}
