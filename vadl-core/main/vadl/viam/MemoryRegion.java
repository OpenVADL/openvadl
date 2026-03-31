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

import java.math.BigInteger;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents a {@code memory region} definition in VIAM.
 * A memory region is defined within a {@link Processor} and may contain a body that
 * writes memory.
 * This class is a subclass of {@link Procedure}, so if there was no body provided
 * in the definition, it falls back to the minimal control flow of a procedure (an empty procedure).
 * The {@link #base()}, {@link #size()} and {@link #holdsFirmware()} properties correspond to
 * the {@code [base], [size] and [firmware]} annotations.
 * If the user didn't set the base, it will be set by the
 * {@link vadl.iss.passes.IssMemoryDetectionPass}, so after this pass ran, the {@link #expectBase()}
 * can be safely used.
 *
 * @see Processor
 * @see Procedure
 * @see vadl.iss.passes.IssMemoryDetectionPass
 */
public class MemoryRegion extends Procedure {

  /**
   * The memory region kind defines properties on how the memory region can be accessed.
   * RAM can always be read and written, ROM is written during initialization but can never
   * be written again.
   */
  public enum Kind {
    RAM,
    ROM,
  }

  private Kind kind;
  private Memory memoryRef;

  @Nullable
  private BigInteger base;
  @Nullable
  private Integer size;
  private boolean holdsFirmware;

  /**
   * Constructs the memory region.
   * All annotation-related properties are set by the annotation provider.
   *
   * @param behavior if no body was provided, this should be a minimal procedure control flow
   *                 (start + end node).
   */
  public MemoryRegion(Identifier identifier, Kind kind,
                      Memory memoryRef, Graph behavior) {
    super(identifier, new Parameter[] {}, behavior);
    this.kind = kind;
    this.memoryRef = memoryRef;
    // things set by annotation provider
    this.base = null;
    this.size = null;
    this.holdsFirmware = false;
  }

  public boolean hasInitialization() {
    return behavior().getNodes().count() > 2;
  }

  public Kind kind() {
    return kind;
  }

  public Memory memoryRef() {
    return memoryRef;
  }

  public @Nullable BigInteger base() {
    return base;
  }

  public @Nullable Integer size() {
    return size;
  }

  public boolean holdsFirmware() {
    return holdsFirmware;
  }

  public BigInteger expectBase() {
    ensure(base != null, "Base was not set");
    return base;
  }

  public void setBase(BigInteger base) {
    this.base = base;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public void setHoldsFirmware(boolean holdsFirmware) {
    this.holdsFirmware = holdsFirmware;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
