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

package vadl.vdt.impl.irregular.vliw.utils.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import vadl.vdt.impl.vliw.model.Counter.BitCounter;
import vadl.vdt.impl.vliw.model.Counter.IntCounter;
import vadl.vdt.impl.vliw.model.PointedExpression;
import vadl.vdt.impl.vliw.model.PointedItem;
import vadl.vdt.impl.vliw.model.PointedItem.Alternation;
import vadl.vdt.impl.vliw.model.PointedItem.Literal;
import vadl.vdt.impl.vliw.model.PointedItem.Permutation;
import vadl.vdt.impl.vliw.model.PointedItem.Repetition;
import vadl.vdt.impl.vliw.model.PointedItem.Sequence;
import vadl.viam.Group;

/**
 * Custom parser for extended pointed regular expressions, used to manually construct intermediate
 * states during testing.
 * <br>
 * The recognized language is intentionally more permissive than group expressions in VADL,
 * to allow for testing of edge cases in the automaton generator. E.g. repetitions allow arbitrary
 * sub-expressions, whereas in VADL they can only contain single Operations.
 * <br>
 * Since the expression language is a bit more powerful, the syntax also differs slightly, e.g.
 * repetitions require their body to be enclosed in parentheses:
 * <pre>
 *   &lt;(A)&lt;1..2>, false>
 * </pre>
 * Compared to VADL, alternative do not need to be enclosed in parentheses. E.g. the group
 * expression <code>(A|B)</code> in VADL, can be expressed here as <code>&lt;A|B, true></code>.
 * <br><br>
 * Grammar:
 * <pre>
 *   &lt;pre ::= "<" &lt;item> "," &lt;trailing> ">"
 *   &lt;item> ::= &lt;seq> ("|" &lt;seq>)*
 *   &lt;seq> ::= &lt;pointed> ("." &lt;pointed>)*
 *   &lt;pointed> ::= "*"? (&lt;rep> | &lt;perm> | &lt;lit>)
 *   &lt;rep> ::= "(" &lt;item> ")" ("[" [0-9]+ "]")? "<" [0-9]+ ".." [0-9]+ ">"
 *   &lt;perm> ::= ("[" [0-9]+ "]")? "{" &lt;item> ("," &lt;item>)+ "}"
 *   &lt;lit> ::= [A-Z]
 *   &lt;trailing> ::= "true" | "false"
 * </pre>
 *
 * @author Matthias Raschhofer
 */
public class PointedExpressionParser {

  private final Collection<Group.Literal> literals;
  private final List<String> tokens;
  int next;

  public PointedExpressionParser(List<String> tokens, Collection<Group.Literal> literals) {
    this.tokens = tokens;
    this.literals = literals;
    this.next = 0;
  }

  public static PointedExpression parse(String expression, Collection<Group.Literal> literals) {
    final var t = Arrays.stream(expression.splitWithDelimiters(
            "([<>,\\|\\*\\(\\)\\-\\{\\}\\[\\]]|\\.\\.|\\.)", 0
        ))
        .filter(s -> !s.isEmpty())
        .toList();
    return new PointedExpressionParser(t, literals).parseExpr();
  }

  /**
   * <pre> ::= "<" <item> "," <trailing> ">"
   */
  private PointedExpression parseExpr() {
    consume("<");
    PointedItem item = parseItem();
    consume(",");
    boolean trailing = parseBool();
    consume(">");
    if (next < tokens.size()) {
      throw new IllegalStateException("Unexpected token at end of stream: " + tokens.get(next));
    }
    return new PointedExpression(item, trailing);
  }

  /**
   * <item> ::= <seq> ("|" <seq>)*
   */
  private PointedItem parseItem() {
    final List<PointedItem> elements = new ArrayList<>();
    elements.add(parseSeq());
    if (peek("|")) {
      consume("|");
      elements.add(parseSeq());
    }

    if (elements.size() == 1) {
      // Avoid redundant wrapper node
      return elements.getFirst();
    }

    return new Alternation(elements);
  }

  /**
   * <seq> ::= <pointed> ("." <pointed>)*
   */
  private PointedItem parseSeq() {
    final List<PointedItem> elements = new ArrayList<>();
    elements.add(parsePointed());
    if (peek(".")) {
      consume(".");
      elements.add(parsePointed());
    }

    if (elements.size() == 1) {
      // Avoid redundant wrapper node
      return elements.getFirst();
    }

    return new Sequence(elements);
  }

  /**
   * <pointed> ::= "*"? (<rep> | <perm> | <lit>)
   */
  private PointedItem parsePointed() {
    boolean hasPoint = peek("*");
    if (hasPoint) {
      consume("*");
    }

    final PointedItem expr;
    if (peek("(")) {
      var rep = parseRep();
      expr = new Repetition(
          rep.expr(), rep.from(), rep.to(), rep.counter(), hasPoint
      );
    } else if (peek("[") || peek("{")) {
      var perm = parsePerm();
      expr = new Permutation(
          perm.elements(), perm.counter(), hasPoint
      );
    } else {
      var lit = parseLit();
      expr = new Literal(lit.lit(), hasPoint);
    }

    return expr;
  }

  /**
   * <rep> ::= "(" <item> ")" ("[" [0-9]+ "]")? "<" [0-9]+ "-" [0-9]+ ">"
   */
  private Repetition parseRep() {
    consume("(");
    var e = parseItem();
    consume(")");

    BigInteger counterId = null;
    if (peek("[")) {
      consume("[");
      counterId = parseBigInt();
      consume("]");
    }

    consume("<");
    var from = parseBigInt();
    consume("..");
    var to = parseBigInt();
    consume(">");
    return new Repetition(e, from, to,
        counterId == null
            // No duplicate detection for now
            ? new IntCounter()
            : new IntCounter(counterId.longValue())
        , false);
  }

  /**
   * <perm> ::= ("[" [0-9]+ "]")? "{" <item> ("," <item>)+ "}"
   */
  private Permutation parsePerm() {

    BigInteger counterId = null;
    if (peek("[")) {
      consume("[");
      counterId = parseBigInt();
      consume("]");
    }

    consume("{");

    final List<PointedItem> elements = new ArrayList<>();

    elements.add(parseItem());
    consume(",");
    elements.add(parseItem());

    if (peek(",")) {
      consume(",");
      elements.add(parseItem());
    }

    consume("}");

    final var width = BigInteger.valueOf(elements.size());
    return new Permutation(
        elements,
        counterId == null
            ? new BitCounter(width)
            : new BitCounter(counterId.longValue(), width),
        false
    );
  }

  /**
   * <lit> ::= [A-Z]
   */
  private Literal parseLit() {
    var l = consume();
    if (!l.matches("[A-Z]")) {
      throw new IllegalArgumentException("Invalid literal '%s' at position %d".formatted(l, next));
    }
    var gl = literals.stream()
        .filter(lit -> lit.op().simpleName().equals(l))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown literal '%s' at position %d"));
    return new Literal(gl, false);
  }

  private BigInteger parseBigInt() {
    var n = consume();
    if (NumberUtils.isDigits(n)) {
      return new BigInteger(n);
    }
    throw new IllegalArgumentException("Invalid integer '%s' at position %d".formatted(n, next));
  }

  private boolean parseBool() {
    var n = consume();
    return switch (n) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalStateException(
          "Expected 'true' or ’false' at position %d, but got '%s'".formatted(next, n));
    };
  }

  private String consume() {
    return tokens.get(next++).trim();
  }

  private void consume(String s) {
    var n = tokens.get(next).trim();
    if (!s.equals(n)) {
      throw new IllegalArgumentException(
          "The parser expected a '%s' at position %d, but got '%s'".formatted(s, next, n));
    }
    next++;
  }

  private String peek() {
    return tokens.get(next).trim();
  }

  private boolean peek(String s) {
    return s.equals(tokens.get(next).trim());
  }

}
