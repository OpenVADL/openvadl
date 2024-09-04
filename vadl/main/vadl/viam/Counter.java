package vadl.viam;

import java.util.concurrent.atomic.AtomicInteger;

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
public sealed abstract class Counter extends Definition {

  public enum Kind {
    PROGRAM_COUNTER,
    GROUP_COUNTER,
  }

  private final Kind kind;

  public Counter(Identifier identifier, Kind kind) {
    super(identifier);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  // the id counter to produce unique counter simple names.
  private static final AtomicInteger idCounter = new AtomicInteger();

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

    public RegisterCounter(Register registerRef, Kind kind) {
      super(registerRef.identifier.append("counter_" + idCounter.incrementAndGet() + "."), kind);
      this.registerRef = registerRef;
    }

    public Register registerRef() {
      return registerRef;
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

    public RegisterFileCounter(RegisterFile registerFileRef, Constant.Value index, Kind kind) {
      super(registerFileRef.identifier.append("counter_" + idCounter.incrementAndGet() + "."),
          kind);
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
  }

}