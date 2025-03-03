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

/**
 * The Counter represents a program or group counter in a VADL specification.
 *
 * <p>There are 3 ways to define a program counter (same for group counters) in VADL:
 * <pre>
 *   1. program counter PC: Bits<32>
 *   2. alias program counter PC: Bits<32> = SOME_REGISTER
 *   3. alias program counter PC: Bits<32> = SOME_REGISTER_FILE( < constant > )
 * </pre>
 * This means, the program counter refers either to a {@link Register} or a {@link RegisterFile}
 * with a index as {@link Constant.Value}. A {@code program counter} definition is stored
 * as a {@link Register} PC and a {@link RegisterCounter} in the VIAM.</p>
 *
 * <p>{@link RegisterCounter} is used to describe a counter that refers to a {@link Register}
 * and {@link RegisterFileCounter} is ued to describe a counter that refers to a
 * {@link RegisterFile}.</p>
 *
 * <p><b>Note:</b> The register and register file is not owned by the counter, but only
 * referenced by it.</p>
 */
public abstract sealed class Counter extends Definition {

  /**
   * The kind of the counter.
   * It is either a program counter or a group counter.
   */
  public enum Kind {
    PROGRAM_COUNTER,
    GROUP_COUNTER;

    @Override
    public String toString() {
      return this == PROGRAM_COUNTER ? "program counter" : "group counter";
    }
  }

  /**
   * The position of the counter.
   *
   * <p>{@code CURRENT} defines the counter to point to the start of the currently defined
   * instruction (group). When no annotation is given this is the default behavior.
   * This mode is the best for ARM AArch64 and RISC-V architectures. {@code NEXT} defines the
   * counter to point to the end of the currently defined instruction (group).
   * This mode is the best for Alpha and MIPS architectures. {@code NEXT_NEXT} defines the program
   * counter to point to the end of the instruction after the currently defined instruction
   * which is required to have the same size of the currently defined instruction.
   * If the sizes are different the behavior is undefined. The {@code NEXT_NEXT} is not
   * valid for group counters.
   */
  public enum Position {
    CURRENT,
    NEXT,
    NEXT_NEXT
  }

  private final Position position;

  private final Kind kind;

  /**
   * Constructs the Counter.
   */
  public Counter(Identifier identifier, Position position, Kind kind) {
    super(identifier);
    this.kind = kind;
    this.position = position;
  }

  public Position position() {
    return position;
  }

  public Kind kind() {
    return kind;
  }

  /**
   * Returns either a {@link Register} or a {@link RegisterFile} that is referenced by this
   * counter.
   */
  public abstract Resource registerResource();

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * A counter that refers to a {@link Register}. This is the case when defining a counter like
   * <pre>
   * program counter PC: Bits<32>
   * // or
   * alias program counter PC: Bits<32> = SOME_REGISTER
   * </pre>
   */
  public static final class RegisterCounter extends Counter {
    // not owned, but referenced. It is owned by the ISA.registers field
    private final Register registerRef;

    /**
     * Constructs the RegisterCounter.
     *
     * @param registerRef the register this counter is represented by
     */
    public RegisterCounter(Identifier identifier, Register registerRef, Position position,
                           Kind kind) {
      super(
          identifier,
          position,
          kind);
      this.registerRef = registerRef;
    }

    public Register registerRef() {
      return registerRef;
    }

    @Override
    public Register registerResource() {
      return registerRef;
    }

    @Override
    public String toString() {
      var result = kind() + " " + identifier + ": " + registerRef.type();
      if (!registerRef.identifier.equals(identifier)) {
        result += " = " + registerRef.identifier;
      }
      return result;
    }
  }

  /**
   * A counter that refers to a {@link RegisterFile} with a index {@link Constant.Value}.
   * This is the case when defining a counter like
   * <pre>
   * alias program counter PC: Bits<32> = SOME_REGISTER_FILE( < constant > )
   * </pre>
   */
  public static final class RegisterFileCounter extends Counter {

    // not owned, but referenced. It is owned by the ISA.registerFiles field
    private final RegisterFile registerFileRef;
    private final Constant.Value index;

    /**
     * Constructs the RegisterFileCounter.
     *
     * @param registerFileRef the register file of the counter-register
     * @param index           the index of the counter-register in the register file
     */
    public RegisterFileCounter(Identifier identifier, RegisterFile registerFileRef,
                               Constant.Value index,
                               Position position, Kind kind) {
      super(identifier, position, kind);
      this.registerFileRef = registerFileRef;
      this.index = index;
    }

    public RegisterFile registerFileRef() {
      return registerFileRef;
    }

    public Constant.Value index() {
      return index;
    }

    @Override
    public void verify() {
      super.verify();
      ensure(registerFileRef.addressType().isTrivialCastTo(index.type()),
          "Index type does not match register file address type. %s vs %s",
          registerFileRef.addressType(), index.type());
    }

    @Override
    public RegisterFile registerResource() {
      return registerFileRef;
    }

    @Override
    public String toString() {
      var result = kind() + " " + identifier + ": " + registerFileRef.resultType();
      result += " = " + registerFileRef.identifier + "(" + index.intValue() + ")";
      return result;
    }
  }

}