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

package vadl.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.DiagnosticList;
import vadl.lsp.DocumentSnapshot;
import vadl.utils.SourceLocation;

/**
 * Tokenizer used by the language server. Provides LSP semantic tokens based on VADL AST.
 */
public class LspAstTokenizer extends LspTokenizer {
  private static final Logger log = LoggerFactory.getLogger(LspAstTokenizer.class);

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
  public LspAstTokenizer(Map<String, Integer> tokenTypesMap,
                         Map<String, Integer> tokenModifiersMap) {
    super(tokenTypesMap, tokenModifiersMap);
  }

  @Override
  public List<Integer> getTokens(DocumentSnapshot snapshot) {
    Ast ast;
    try {
      ast = VadlParser.parse(snapshot.text(), URI.create(snapshot.uri()));
    } catch (DiagnosticList dl) {
      log.debug("Cannot produce tokens because Parser returned diagnostics instead");
      return List.of();
    }

    Visitor visitor = new Visitor(ast);
    return transformToLspTokens(visitor.tokenize(), snapshot);
  }

  private List<Integer> transformToLspTokens(@Nullable Token token, DocumentSnapshot snapshot) {
    if (token == null) {
      return List.of();
    }
    // All Tokens are single-line at this point

    List<Integer> lspTokens = new ArrayList<>();
    int previousLine = 1; // Token.beginLine starts at 1
    int previousCol = 1; // Same for Token.beginColumn
    for (; token != null; token = token.next) {
      if (token.tokenType == null) {
        continue;
      }
      int tokenType = getTokenTypeInteger(token.tokenType);
      if (tokenType < 0) {
        continue;
      }

      int length;
      if (token.endColumn == -1) {
        length = snapshot.getUtf8LineLength(token.endLine - 1) - token.beginColumn + 1;
      } else {
        length = token.endColumn - token.beginColumn + 1;
      }
      if (length <= 0) {
        continue;
      }

      int deltaLine = token.beginLine - previousLine;
      previousLine = token.beginLine;

      if (deltaLine != 0) {
        previousCol = 1;
      }
      int deltaStart = token.beginColumn - previousCol;
      previousCol = token.beginColumn;

      // deltaLine, deltaStart, length, tokenType, tokenModifiers
      lspTokens.add(deltaLine);
      lspTokens.add(deltaStart);
      lspTokens.add(length);
      lspTokens.add(tokenType);
      lspTokens.add(0);
    }
    return lspTokens;
  }


  private static class Visitor extends RecursiveAstVisitor {
    private final Ast ast;
    private final List<Token> tokenList = new ArrayList<>();

    Visitor(Ast ast) {
      this.ast = ast;
    }

    @Nullable Token tokenize() {
      for (var definition : ast.definitions) {
        definition.accept(this);
      }
      Collections.sort(tokenList);

      Token first = Token.createLinkedList(tokenList);
      Token token = first;
      while (token != null) {
        token = normalizeToken(token).next;
      }

      return first;
    }

    private Token normalizeToken(Token token) {
      // Token may be followed by other tokens that overlap part of it, as they have been created
      // from AST nodes that are contained within the node responsible for the current token. These
      // overlaps must be resolved.

      Token addToNextLine = token.splitMultiline();
      // From now on, token is single-line
      if (token.next == null) {
        token.next = addToNextLine;
        return token;
      }

      if (token.next.beginLine == token.beginLine
          && (token.endColumn == -1 || token.next.beginColumn <= token.endColumn)) {

        // Overlaps with next token
        Token trailingToken = token.extract(token.next);
        token = normalizeToken(token.next);
        if (trailingToken != null) {
          token.insertAfter(trailingToken);
          // Immediately process trailing token, or order will be messed up
          // (for trailing tokens: FIFO)
          token = normalizeToken(trailingToken);
        }
      }

      // Must be done here, as doing this sooner messes up the order (via recursive normalizeToken()
      // calls above). (for added to next line: LIFO)
      if (addToNextLine != null) {
        Token current = token;
        while (current.next != null && current.next.beginLine < addToNextLine.beginLine) {
          current = current.next;
        }
        current.insertAfter(addToNextLine);
      }

      return token;
    }

    private void processNode(Node node) {
      log.debug("{} @{}", node, node.location());
      try {
        tokenList.add(new Token(node.location(), getTokenType(node)));
      } catch (IllegalArgumentException e) {
        // Ignore Node with invalid location
      }

      if (node instanceof IdentifiableNode identifiableNode) {
        processNode(identifiableNode.identifier());
      }
    }

    @Override
    protected void beforeTravel(Expr expr) {
      processNode(expr);
    }

    @Override
    protected void beforeTravel(Statement statement) {
      processNode(statement);
    }

    @Override
    protected void beforeTravel(Definition definition) {
      processNode(definition);
    }

    private @Nullable String getTokenType(Node node) {
      if (node instanceof Identifier) {
        return SemanticTokenTypes.Variable;
      }

      if (node instanceof BinOp || node instanceof UnOp) {
        return SemanticTokenTypes.Operator;
      }

      if (node instanceof IntegerLiteral) {
        return SemanticTokenTypes.Number;
      }
      if (node instanceof StringLiteral) {
        return SemanticTokenTypes.String;
      }

      // Everything else should be Keyword
      return SemanticTokenTypes.Keyword;
    }
  }


  private static class Token implements Comparable<Token> {
    /**
     * 1-based beginning line number.
     */
    int beginLine;
    /**
     * 1-based position within the beginning line.
     */
    int beginColumn;
    /**
     * 1-based end line number (inclusive).
     */
    int endLine;
    /**
     * 1-based end position within the end line (inclusive). -1: end-of-line (i.e. last character
     * before newline character).
     */
    int endColumn;
    /**
     * On of the constants from {@link org.eclipse.lsp4j.SemanticTokenTypes}.
     */
    @Nullable
    String tokenType;

    @Nullable
    Token next = null;

    Token(int beginLine, int beginColumn, int endLine, int endColumn, @Nullable String tokenType) {
      // Protecting against bogus locations the AST may containt
      if (beginLine < 1 || beginColumn < 1 || endLine < 1 || (endColumn < 1 && endColumn != -1)) {
        throw new IllegalArgumentException();
      }
      this.beginLine = beginLine;
      this.beginColumn = beginColumn;
      this.endLine = endLine;
      this.endColumn = endColumn;
      this.tokenType = tokenType;
    }

    Token(SourceLocation location, @Nullable String tokenType) {
      this(location.begin().line(), location.begin().column(), location.end().line(),
          location.end().column(), tokenType);
    }

    static @Nullable Token createLinkedList(List<Token> tokenList) {
      Token current = null;
      for (Token t : tokenList.reversed()) {
        t.next = current;
        current = t;
      }
      return current;
    }

    @Nullable Token splitMultiline() {
      if (beginLine == endLine) {
        return null;
      }

      Token newToken = new Token(beginLine + 1, 1, endLine, endColumn, tokenType);
      endLine = beginLine;
      endColumn = -1; // aka end-of-line

      return newToken;
    }

    @Nullable Token extract(Token token) {
      Token trailingToken = null;
      if (this.endLine > token.endLine || (this.endLine == token.endLine
          && token.endColumn != -1 && (this.endColumn == -1 || this.endColumn > token.endColumn))) {

        trailingToken = new Token(token.endLine, token.endColumn + 1,
            this.endLine, this.endColumn, this.tokenType);
      }

      this.endLine = token.beginLine;
      this.endColumn = token.beginColumn - 1;

      return trailingToken;
    }

    void insertAfter(Token token) {
      token.next = this.next;
      this.next = token;
    }

    @Override
    public String toString() {
      return (tokenType != null ? tokenType : "?") + " " + beginLine + ":" + beginColumn + ".."
          + endLine + ":" + endColumn;
    }

    @Override
    public int compareTo(@Nonnull Token o) {
      // First start position first
      if (this.beginLine != o.beginLine) {
        return this.beginLine - o.beginLine;
      }
      if (this.beginColumn != o.beginColumn) {
        return this.beginColumn - o.beginColumn;
      }

      // Last end position first
      if (this.endLine != o.endLine) {
        return o.endLine - this.endLine;
      }
      if (this.endColumn != o.endColumn) {
        return o.endColumn - this.endColumn;
      }

      return 0;
    }
  }
}
