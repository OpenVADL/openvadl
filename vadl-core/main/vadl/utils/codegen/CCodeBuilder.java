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

package vadl.utils.codegen;

import java.util.function.Consumer;

/**
 * A builder interface for generating C code with common control flow structures.
 * Extends {@link CodeGeneratorAppendable} to provide specialized methods for C constructs.
 */
public interface CCodeBuilder extends CodeGeneratorAppendable {

  default CCodeBuilder stmt(String stmt) {
    append(stmt).appendLn(";");
    return this;
  }

  default CCodeBuilder varDecl(String type, String name) {
    append(type).append(" ").append(name).appendLn(";");
    return this;
  }

  default CCodeBuilder varDecl(String type, String name, String value) {
    append(type).append(" ").append(name).append(" = ").append(value).appendLn(";");
    return this;
  }

  /**
   * Generates a C for loop with the given parameters.
   *
   * @param start the initialization expression (e.g., "int i = 0")
   * @param end   the condition expression (e.g., "i < 10")
   * @param step  the increment expression (e.g., "i++")
   * @param body  the body of the loop
   */
  default CCodeBuilder forLoop(String start, String end, String step, Runnable body) {
    append("for (").append(start).append("; ").append(end).append("; ").append(step).append(")")
        .appendLn(" {").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C for loop with the given parameters and a consumer-based body.
   *
   * @param start       the initialization expression (e.g., "int i = 0")
   * @param end         the condition expression (e.g., "i < 10")
   * @param step        the increment expression (e.g., "i++")
   * @param bodyBuilder a consumer that receives this builder to append the loop body
   */
  default CCodeBuilder forLoop(String start, String end, String step,
                               Consumer<CCodeBuilder> bodyBuilder) {
    append("for (").append(start).append("; ").append(end).append("; ").append(step).append(")")
        .appendLn(" {").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a simple C for loop counting from 0 to upperBound (exclusive).
   *
   * @param varName    the loop variable name
   * @param upperBound the upper bound (exclusive)
   * @param body       the body of the loop
   */
  default CCodeBuilder forLoop(String varName, int upperBound, Runnable body) {
    return forLoop("int " + varName + " = 0", varName + " < " + upperBound, varName + "++", body);
  }

  /**
   * Generates a simple C for loop counting from 0 to upperBound (exclusive).
   *
   * @param varName     the loop variable name
   * @param upperBound  the upper bound (exclusive)
   * @param bodyBuilder a consumer that receives this builder to append the loop body
   */
  default CCodeBuilder forLoop(String varName, int upperBound,
                               Consumer<CCodeBuilder> bodyBuilder) {
    return forLoop("int " + varName + " = 0", varName + " < " + upperBound, varName + "++",
        bodyBuilder);
  }

  /**
   * Generates a C while loop.
   *
   * @param condition the loop condition
   * @param body      the body of the loop
   */
  default CCodeBuilder whileLoop(String condition, Runnable body) {
    append("while (").append(condition).appendLn(") {").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C while loop with a consumer-based body.
   *
   * @param condition   the loop condition
   * @param bodyBuilder a consumer that receives this builder to append the loop body
   */
  default CCodeBuilder whileLoop(String condition, Consumer<CCodeBuilder> bodyBuilder) {
    append("while (").append(condition).appendLn(") {").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C do-while loop.
   *
   * @param condition the loop condition
   * @param body      the body of the loop
   */
  default CCodeBuilder doWhileLoop(String condition, Runnable body) {
    appendLn("do {").indent();
    body.run();
    unindent().append("} while (").append(condition).appendLn(");");
    return this;
  }

  /**
   * Generates a C do-while loop with a consumer-based body.
   *
   * @param condition   the loop condition
   * @param bodyBuilder a consumer that receives this builder to append the loop body
   */
  default CCodeBuilder doWhileLoop(String condition, Consumer<CCodeBuilder> bodyBuilder) {
    appendLn("do {").indent();
    bodyBuilder.accept(this);
    unindent().append("} while (").append(condition).appendLn(");");
    return this;
  }

  /**
   * Generates a C if statement.
   *
   * @param condition the condition expression
   * @param body      the body of the if statement
   */
  default CCodeBuilder ifStmt(String condition, Runnable body) {
    append("if (").append(condition).appendLn(") {").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C if statement with a consumer-based body.
   *
   * @param condition   the condition expression
   * @param bodyBuilder a consumer that receives this builder to append the if body
   */
  default CCodeBuilder ifStmt(String condition, Consumer<CCodeBuilder> bodyBuilder) {
    append("if (").append(condition).appendLn(") {").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C if-else statement.
   *
   * @param condition the condition expression
   * @param thenBody  the body of the if clause
   * @param elseBody  the body of the else clause
   */
  default CCodeBuilder ifElseStmt(String condition, Runnable thenBody, Runnable elseBody) {
    append("if (").append(condition).appendLn(") {").indent();
    thenBody.run();
    unindent().appendLn("} else {").indent();
    elseBody.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C if-else statement with consumer-based bodies.
   *
   * @param condition       the condition expression
   * @param thenBodyBuilder a consumer that receives this builder to append the if body
   * @param elseBodyBuilder a consumer that receives this builder to append the else body
   */
  default CCodeBuilder ifElseStmt(String condition, Consumer<CCodeBuilder> thenBodyBuilder,
                                  Consumer<CCodeBuilder> elseBodyBuilder) {
    append("if (").append(condition).appendLn(") {").indent();
    thenBodyBuilder.accept(this);
    unindent().appendLn("} else {").indent();
    elseBodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Starts an else-if chain after an if statement.
   *
   * @param condition the condition expression
   * @param body      the body of the else-if clause
   */
  default CCodeBuilder elseIfStmt(String condition, Runnable body) {
    append(" else if (").append(condition).appendLn(") {").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Starts an else-if chain after an if statement with a consumer-based body.
   *
   * @param condition   the condition expression
   * @param bodyBuilder a consumer that receives this builder to append the else-if body
   */
  default CCodeBuilder elseIfStmt(String condition, Consumer<CCodeBuilder> bodyBuilder) {
    append(" else if (").append(condition).appendLn(") {").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates an else clause.
   *
   * @param body the body of the else clause
   */
  default CCodeBuilder elseStmt(Runnable body) {
    appendLn(" else {").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates an else clause with a consumer-based body.
   *
   * @param bodyBuilder a consumer that receives this builder to append the else body
   */
  default CCodeBuilder elseStmt(Consumer<CCodeBuilder> bodyBuilder) {
    appendLn(" else {").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C switch statement.
   *
   * @param expression the expression to switch on
   * @param body       the body of the switch statement (containing case statements)
   */
  default CCodeBuilder switchStmt(String expression, Runnable body) {
    append("switch (").append(expression).appendLn(") {").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C switch statement with a consumer-based body.
   *
   * @param expression  the expression to switch on
   * @param bodyBuilder a consumer that receives this builder to append case statements
   */
  default CCodeBuilder switchStmt(String expression, Consumer<CCodeBuilder> bodyBuilder) {
    append("switch (").append(expression).appendLn(") {").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C case label.
   *
   * @param value the case value
   */
  default CCodeBuilder caseLabel(String value) {
    append("case ").append(value).appendLn(":");
    return this;
  }

  /**
   * Generates a C case label with a body.
   *
   * @param value the case value
   * @param body  the body of the case
   */
  default CCodeBuilder caseStmt(String value, Runnable body) {
    append("case ").append(value).appendLn(":").indent();
    body.run();
    return (CCodeBuilder) unindent();
  }

  /**
   * Generates a C case label with a consumer-based body.
   *
   * @param value       the case value
   * @param bodyBuilder a consumer that receives this builder to append the case body
   */
  default CCodeBuilder caseStmt(String value, Consumer<CCodeBuilder> bodyBuilder) {
    append("case ").append(value).appendLn(":").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent();
  }

  /**
   * Generates a C default case label.
   */
  default CCodeBuilder defaultLabel() {
    return (CCodeBuilder) appendLn("default:");
  }

  /**
   * Generates a C default case with a body.
   *
   * @param body the body of the default case
   */
  default CCodeBuilder defaultCase(Runnable body) {
    appendLn("default:").indent();
    body.run();
    return (CCodeBuilder) unindent();
  }

  /**
   * Generates a C default case with a consumer-based body.
   *
   * @param bodyBuilder a consumer that receives this builder to append the default case body
   */
  default CCodeBuilder defaultCase(Consumer<CCodeBuilder> bodyBuilder) {
    appendLn("default:").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent();
  }

  /**
   * Generates a C break statement.
   */
  default CCodeBuilder breakStmt() {
    appendLn("break;");
    return this;
  }

  /**
   * Generates a C continue statement.
   */
  default CCodeBuilder continueStmt() {
    appendLn("continue;");
    return this;
  }

  /**
   * Generates a C return statement without a value.
   */
  default CCodeBuilder returnStmt() {
    appendLn("return;");
    return this;
  }

  /**
   * Generates a C return statement with a value.
   *
   * @param value the return value expression
   */
  default CCodeBuilder returnStmt(String value) {
    append("return ").append(value).appendLn(";");
    return this;
  }

  /**
   * Generates a C goto statement.
   *
   * @param label the label to jump to
   */
  default CCodeBuilder gotoStmt(String label) {
    append("goto ").append(label).appendLn(";");
    return this;
  }

  /**
   * Generates a C label.
   *
   * @param label the label name
   */
  default CCodeBuilder label(String label) {
    append(label).appendLn(":");
    return this;
  }

  /**
   * Generates a C code block with braces.
   *
   * @param body the body of the block
   */
  default CCodeBuilder block(Runnable body) {
    appendLn("{").indent();
    body.run();
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C code block with braces and a consumer-based body.
   *
   * @param bodyBuilder a consumer that receives this builder to append the block body
   */
  default CCodeBuilder block(Consumer<CCodeBuilder> bodyBuilder) {
    appendLn("{").indent();
    bodyBuilder.accept(this);
    return (CCodeBuilder) unindent().appendLn("}");
  }

  /**
   * Generates a C function call.
   *
   * @param functionName the name of the function
   * @param args         the function arguments
   */
  default CCodeBuilder call(String functionName, String... args) {
    append(functionName).append("(");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        append(", ");
      }
      append(args[i]);
    }
    append(")");
    return this;
  }

  /**
   * Generates a C function call statement with semicolon.
   *
   * @param functionName the name of the function
   * @param args         the function arguments
   */
  default CCodeBuilder callStmt(String functionName, String... args) {
    call(functionName, args);
    appendLn(";");
    return this;
  }

  /**
   * Generates a C single-line comment.
   *
   * @param comment the comment text
   */
  default CCodeBuilder comment(String comment) {
    append("// ").appendLn(comment);
    return this;
  }

  /**
   * Generates a C multi-line comment.
   *
   * @param lines the comment lines
   */
  default CCodeBuilder multiLineComment(String... lines) {
    appendLn("/*");
    for (String line : lines) {
      append(" * ").appendLn(line);
    }
    appendLn(" */");
    return this;
  }

}
