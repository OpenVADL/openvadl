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

package vadl.cppCodeGen.context;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.FormatMethod;
import java.util.function.Consumer;
import vadl.types.DataType;
import vadl.types.Type;


/**
 * The context used by code generation handlers to submit generated code.
 * It allows handlers to write strings, and call generation on sub nodes,
 * by using the {@link #gen(T)} method.
 * All written strings are passed to the writer (a String consumer).
 */
public abstract class CGenContext<T> {

  protected String prefix = "";
  protected final Consumer<String> writer;

  public CGenContext(
      Consumer<String> writer,
      String prefix
  ) {
    this.writer = writer;
    this.prefix = prefix;
  }

  /**
   * Generate code for the sub entity.
   *
   * @param entity the entity that should be turned into C code.
   * @return THIS
   */
  public abstract CGenContext<T> gen(T entity);

  /**
   * Generate code for the sub entity and return the generated code as string.
   *
   * @param entity the entity that should be turned into C code.
   * @return THIS
   */
  public abstract String genToString(T entity);

  /**
   * Enters tab mode and every following write is tabbed until {@code tabOut} is called.
   */
  public CGenContext<T> spacedIn() {
    prefix = "   ";
    return this;
  }

  /**
   * Exits tab mode and every following write is not tabbed anymore.
   */
  public CGenContext<T> spaceOut() {
    prefix = "";
    return this;
  }

  /**
   * Write C code to generation context.
   */
  public CGenContext<T> wr(String str) {
    writer.accept(prefix + str);
    return this;
  }

  /**
   * Write C code to generation context.
   *
   * @param fmt  A string format
   * @param args Arguments for placeholders in the format string
   */
  @FormatMethod
  public CGenContext<T> wr(String fmt, Object... args) {
    wr(String.format(prefix + fmt, args));
    return this;
  }

  /**
   * Write C code to generation context with a newline.
   *
   * @param fmt  A string format
   * @param args Arguments for placeholders in the format string
   */
  @FormatMethod
  public CGenContext<T> ln(String fmt, Object... args) {
    wr(String.format(prefix + fmt + '\n', args));
    return this;
  }

  /**
   * Write C code to generation context and end with a new line.
   */
  public CGenContext<T> ln(String str) {
    wr(prefix + str + "\n");
    return this;
  }

  /**
   * Write a new line to the generation context.
   */
  public CGenContext<T> ln() {
    wr(prefix + "\n");
    return this;
  }


  /**
   * Get the next fitting C type of the given VADL type.
   */
  @SuppressWarnings("MethodName")
  public DataType cTypeOf(Type type) {
    return requireNonNull(type.asDataType().fittingCppType());
  }

}
