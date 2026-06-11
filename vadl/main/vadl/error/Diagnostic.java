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

package vadl.error;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.Contract;
import vadl.utils.Pair;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;


/**
 * A shared diagnostic instance for warnings and errors in the vadl system.
 *
 * <p>The diagnostics are used to report to the user that something went wrong or can be improved,
 * they are the main communication method between the vadl tool and user.
 *
 * <p><strong>Structure</strong>
 * The structure a diagnostic are strongly inspired by
 * <a href="https://rustc-dev-guide.rust-lang.org/diagnostics.html?highlight=diagnostic#diagnostic-structure">Rust's diagnostics</a>
 * which in our mind gets a lot of this right.
 * <pre>
 * error: Redeclaration of variable             // The level followed by the reason
 *     ╭──[file.vadl:6:19]                      // The file and line of the error
 *     │
 *   6 │ constant flo = 25                      // The primary location of the multilocation
 *     │          ^^^ Second usage              // A message attached to the primary location
 *     │              note: this isn't ok       // A "note" message attached to the primary location
 *     ⋮
 *   3 │ constant flo = 18                      // A secondary location of the multilocation
 *     │          --- First definition          // Which also has a message
 *     │
 *     ├─ From these 2 model invocation (outermost call first): // This code was generated from a
 *     │    1) file.vadl:11:2                   // The first incocation started here
 *     │       file.vadl:9:4                    // Then called the actual model here
 *     │    2) file.vadl:14:2                   // A second invocation started here
 *     │
 *     │
 *     Each variable can only be declared once. // Messages can also belong to the whole diagnostic
 *     help: Find another awesome name.
 * </pre>
 * While they can be thrown on their own, which makes sense in cases where the error is so severe
 * that execution cannot continue, it often makes sense to defer throwing and collect multiple
 * errors to give the user as much information as possible. In those cases the
 * {@link DiagnosticList} can store multiple diagnostics and throw them at once.
 */
public class Diagnostic extends RuntimeException {
  public final Level level;
  public final String reason;
  public final MultiLocation multiLocation;
  public final List<Message> messages;

  /**
   * The same diagnostic can be created from multiple macro invocations. They might get collapsed
   * into one diagnostic, in which case this field is used to store them all.
   *
   * <p>Let's start first with the inner List which is a backtrace like you get in Java for
   * stacktraces, except this tracs macro invocation chains and not funciton calls. Also, unlike
   * Java, the innermost call is last.
   *
   * <p>The outer list is now of such backtraces. In VADL it is quite common that macro's get
   * invoked multiple times, somtimes even hundrets of times. Since the parser already expands the
   * calls the rest of the compiler pipeline will emit virtually the same diagnostic for each
   * invocation, with only the {@code expandedFrom} of the source location being different.
   * The method {@link #collapseSimilar(List)} then can be used to collapse all similar diagnostics,
   * that only differ in the macro trace. This way we can only emit a sinlge diagnostic for our
   * users even and print all the traces where it got invoked from at once.
   *
   * <b>Example</b>
   * <pre>
   * model badboi(name: Id): Defs = {
   *   constant $name: Bits<32> = "Not a bits"
   * }
   *
   * model middle(name: Id): Defs = {
   *   $badboi($name)
   * }
   *
   * model outer(name: Id): Defs = {
   *   $middle($name)
   * }
   *
   * $outer(apfel)
   * $middle(strudel)
   * $badboi(minister)
   * </pre>
   *
   * <p>Which after collapsing the diagnostics will show as:
   * <pre>
   * error: Type Mismatch
   *   ╭── example.vadl:2:30
   *   │
   * 2 │   constant $name: Bits<32> = "Not a bits"
   *   │                              ^^^^^^^^^^^^ Expected Bits<32> but got String.
   *   │
   *   ├─ From these 3 model invocations (outermost call first):
   *   │    1) example.vadl:13:1
   *   │       example.vadl:10:3
   *   │       example.vadl:6:3
   *   │    2) example.vadl:14:1
   *   │       example.vadl:6:3
   *   │    3) example.vadl:15:1
   *   │
   * </pre>
   */
  public final List<List<SourceLocation.DirectLocation>> macroTraces;

  /**
   * It's generally recommended to not instantiate Diagnostics on their own but to make use of the
   * {@link Diagnostic#error(String, WithLocation)} and
   * {@link Diagnostic#warning(String, WithLocation)}
   * builders.
   */
  public Diagnostic(Level level, String reason, MultiLocation multiLocation,
                    List<Message> messages) {
    this.level = level;
    this.reason = reason;
    this.multiLocation = multiLocation;
    this.messages = messages;
    this.macroTraces = new ArrayList<>();

    var initialTrace = multiLocation.primaryLocation.location.fullExpandedFromStack().reversed();
    initialTrace.removeLast();
    if (!initialTrace.isEmpty()) {
      this.macroTraces.add(initialTrace);
    }
  }

  /**
   * Creates a {@link DiagnosticBuilder} for an error that already has all the mandatory fields
   * filled in.
   *
   * @param reason   for the error.
   * @param location where the error occurred (primary location).
   * @return the builder.
   */
  public static DiagnosticBuilder error(String reason, WithLocation location) {
    return new DiagnosticBuilder(Level.ERROR, reason, location.location());
  }


  /**
   * Creates a {@link DiagnosticBuilder} for a warning that already has all the mandatory fields
   * filled in.
   *
   * @param reason   for the warning.
   * @param location where the warning occurred (primary location).
   * @return the builder.
   */
  public static DiagnosticBuilder warning(String reason, WithLocation location) {
    return new DiagnosticBuilder(Level.WARNING, reason, location.location());
  }

  /**
   * Ensures the given condition.
   * If the condition is false, an error defined by the builder lambda is thrown.
   *
   * <p>
   * Example:
   * <pre>{@code
   * ensure(current instanceof InstrEndNode, () ->
   *     error("Instruction contains unsupported features.",
   *         insn.identifier.sourceLocation())
   * );}
   * </pre>
   * </p>
   *
   * @param condition to be checked
   * @param builder   that builds error if condition is false
   */
  @Contract("false, _ -> fail")
  public static void ensure(boolean condition, Supplier<DiagnosticBuilder> builder) {
    if (!condition) {
      throw builder.get().build();
    }
  }

  /**
   * Diagnostics that only differ in the macro trace will get collapsed into one.
   *
   * <p>A detailed explaination of how and why can be found in the documentation of
   * {@link #macroTraces}.
   *
   * @param diagnostics to collapse.
   * @return the deflated diagnostics.
   */
  public static List<Diagnostic> collapseSimilar(List<Diagnostic> diagnostics) {
    var structure = new LinkedHashMap<SourceLocation.DirectLocation, List<Diagnostic>>();

    for (var diagnostic : diagnostics) {

      final var locationDiagnostics = structure.computeIfAbsent(
          diagnostic.multiLocation.primaryLocation.location().asDirectLocation(),
          k -> new ArrayList<>());

      var similar = locationDiagnostics.stream()
          .filter(d -> d.equalsIgnoringMacroTraces(diagnostic))
          .findFirst();

      similar.ifPresentOrElse(
          d -> d.macroTraces.addAll(diagnostic.macroTraces),
          () -> locationDiagnostics.add(diagnostic)
      );
    }


    return structure.values().stream().flatMap(locationDiagnostics -> locationDiagnostics.stream())
        .toList();
  }

  @Override
  public String getMessage() {
    var sb = new StringBuilder();
    // [<LEVEL>] <reason>:
    sb.append("[").append(level).append("]")
        .append(" ").append(reason).append("\n");
    //   - [<TYPE>] <message-1>
    //   - ...
    messages.forEach(
        m -> sb.append("\t- ").append("[").append(m.type).append("]").append(" ").append(m.content)
            .append("\n"));
    //   - [PRIMARY] <primary-location>
    //      - <primary-message-1>
    //      - <primary-message-n>
    sb.append("\t- [PRIMARY] ").append(multiLocation.primaryLocation.location.toConciseString())
        .append("\n");
    multiLocation.primaryLocation.labels.forEach(m -> {
      sb.append("\t\t- ").append("[").append(m.type).append("]").append(" ").append(m.content)
          .append("\n");
    });
    //   - [SECONDARY] <secondary-1-location>
    //      - <secondary-1-message-1>
    //      - <secondary-2-message-n>
    //   - ...
    multiLocation.secondaryLocations.forEach(ll -> {
      sb.append("\t- [SECONDARY] ").append(ll.location.toConciseString()).append("\n");
      ll.labels.forEach(m -> {
        sb.append("\t\t- ").append("[").append(m.type).append("]").append(" ").append(m.content)
            .append("\n");
      });
    });
    return sb.toString();
  }

  /**
   * Checks if two diagnostics are equal ignoring the macro traces.
   *
   * @param other diagnostic to compare to.
   * @return true if the diagnostics are equal, false otherwise.
   */
  public boolean equalsIgnoringMacroTraces(Diagnostic other) {
    return this.level == other.level
        && this.reason.equals(other.reason)
        && this.multiLocation.equalsIgnoringExpandedFrom(other.multiLocation)
        && this.messages.equals(other.messages);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Diagnostic that = (Diagnostic) o;
    return level == that.level && reason.equals(that.reason)
        && multiLocation.equals(that.multiLocation) && messages.equals(that.messages);
  }

  @Override
  public int hashCode() {
    int result = level.hashCode();
    result = 31 * result + reason.hashCode();
    result = 31 * result + multiLocation.hashCode();
    result = 31 * result + messages.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return getMessage();
  }

  /**
   * A bundle of multiple location with messages attached to them.
   *
   * @param primaryLocation    where the warning/error occurred.
   * @param secondaryLocations many multiple locations to provide more context.
   */
  public record MultiLocation(LabeledLocation primaryLocation,
                              List<LabeledLocation> secondaryLocations
  ) {

    /**
     * Finds the labeled location of a plain location or inserts it.
     *
     * @param location to find or insert.
     * @return the found or created labled locaiton.
     */
    public LabeledLocation getOrInsert(SourceLocation location) {
      if (location.equals(primaryLocation.location)) {
        return primaryLocation;
      }

      var secondary =
          secondaryLocations.stream().filter(ll -> ll.location.equals(location)).findFirst();

      if (secondary.isPresent()) {
        return secondary.get();
      }

      var labeledLocation = new LabeledLocation(location, new ArrayList<>());
      secondaryLocations.add(labeledLocation);
      return labeledLocation;
    }

    /**
     * Checks if two multilocations are equal ignoring the expandedFrom stack.
     *
     * @param other to compare to.
     * @return true if the multilocations are equal, false otherwise.
     */
    public boolean equalsIgnoringExpandedFrom(MultiLocation other) {
      if (this.secondaryLocations.size() != other.secondaryLocations.size()) {
        return false;
      }

      return this.primaryLocation.equalsIgnoringExpandedFrom(other.primaryLocation)
          && Streams.zip(this.secondaryLocations.stream(), other.secondaryLocations.stream(),
              (a, b) -> Pair.of(a, b))
          .allMatch(pair -> pair.left().equalsIgnoringExpandedFrom(pair.right()));
    }
  }

  /**
   * A Location which is annotated with zero or more labels.
   *
   * @param location to be annotated
   * @param labels   to annotate
   */
  public record LabeledLocation(SourceLocation location, List<Message> labels) {

    /**
     * Checks if two locations are equal ignoring the expandedFrom stack.
     *
     * @param other to compare to.
     * @return true if the locations are equal, false otherwise.
     */
    public boolean equalsIgnoringExpandedFrom(LabeledLocation other) {
      return this.location.equalsIgnoringExpandedFrom(other.location)
          && this.labels.equals(other.labels);
    }
  }

  /**
   * A message (also includes help and notes) which can be part of the error itself or pinned to a
   * location.
   *
   * @param type    of the message
   * @param content the message says
   */
  public record Message(MsgType type, String content) {
  }

  /**
   * Help should be used to show changes the user can possibly make to fix the problem.
   * Note should be used for everything else, such as other context, information and facts,
   * online resources to read, etc.
   */
  public enum MsgType {
    NOTE,
    HELP,
    PLAIN
  }

  /**
   * The level/severity of a diagnostic.
   */
  public enum Level {
    ERROR,
    WARNING,
  }
}