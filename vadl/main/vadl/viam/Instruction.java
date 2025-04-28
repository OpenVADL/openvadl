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

package vadl.viam;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import vadl.viam.graph.Graph;

/**
 * The VADL ISA Instruction definition.
 */
// TODO: Instruction should have information about source and destination registers
//  (not from AST, computed by analysis).
public class Instruction extends Definition implements DefProp.WithBehavior, PrintableInstruction {

  private final Graph behavior;
  private final Assembly assembly;
  private final Encoding encoding;

  @LazyInit
  private InstructionSetArchitecture parentArchitecture;

  /**
   * Set during the {@link vadl.viam.passes.InstructionResourceAccessAnalysisPass}.
   */
  @Nullable
  private Set<Resource> writtenResources;
  /**
   * Set during the {@link vadl.viam.passes.InstructionResourceAccessAnalysisPass}.
   */
  @Nullable
  private Set<Resource> readResources;

  /**
   * Creates an Instruction object with the given parameters.
   *
   * @param identifier The identifier of the instruction.
   * @param behavior   The behaviors graph of the instruction.
   * @param assembly   The assembly of the instruction.
   * @param encoding   The encoding of the instruction.
   */
  public Instruction(
      Identifier identifier,
      Graph behavior,
      Assembly assembly,
      Encoding encoding
  ) {
    super(identifier);
    this.behavior = behavior;
    this.assembly = assembly;
    this.encoding = encoding;

    behavior.setParentDefinition(this);
  }

  @Override
  public Identifier identifier() {
    return identifier;
  }

  @Override
  public Graph behavior() {
    return behavior;
  }

  @Override
  public Assembly assembly() {
    return assembly;
  }

  public Encoding encoding() {
    return encoding;
  }

  public Format format() {
    return encoding.format();
  }

  /**
   * Returns the {@link Resource}s that are written by this instruction.
   */
  public Set<Resource> writtenResources() {
    ensure(writtenResources != null,
        "No read resources set. "
            + "The InstructionResourceAccessAnalysisPass has to run before accessing this.");
    return writtenResources;
  }

  /**
   * Returns the {@link Resource}s that are read by this instruction.
   */
  public Set<Resource> readResources() {
    ensure(readResources != null,
        "No read resources set. "
            + "The InstructionResourceAccessAnalysisPass has to run before accessing this.");
    return readResources;
  }

  // this is set by InstructionSetArchitecture the Instruction is added to
  void setParentArchitecture(InstructionSetArchitecture parentArchitecture) {
    this.parentArchitecture = parentArchitecture;
  }

  public InstructionSetArchitecture parentArchitecture() {
    return parentArchitecture;
  }

  @Override
  public void verify() {
    super.verify();

    ensure(behavior.isInstruction(), "Behavior is not a valid instruction behaviors");

    behavior.verify();
  }


  /**
   * Used by the {@link vadl.viam.passes.InstructionResourceAccessAnalysisPass}.
   */
  public void setWrittenResources(@NonNull Set<Resource> writtenResources) {
    this.writtenResources = writtenResources;
  }

  /**
   * Used by the {@link vadl.viam.passes.InstructionResourceAccessAnalysisPass}.
   */
  public void setReadResources(@NonNull Set<Resource> readResources) {
    this.readResources = readResources;
  }

  @Override
  public String toString() {
    return identifier.name() + ": " + format().identifier.name();
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public List<Graph> behaviors() {
    return List.of(behavior);
  }
}
