package vadl.viam;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.Graph;

/**
 * The VADL ISA Instruction definition.
 */
// TODO: Instruction should have information about source and destination registers
//  (not from AST, computed by analysis).
public class Instruction extends Definition implements DefProp.WithBehavior {

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
   * @param behavior   The behavior graph of the instruction.
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

  public Graph behavior() {
    return behavior;
  }

  public Assembly assembly() {
    return assembly;
  }

  public Encoding encoding() {
    return encoding;
  }

  public Format format() {
    return encoding.format();
  }

  public @Nullable Set<Resource> writtenResources() {
    return writtenResources;
  }

  public @Nullable Set<Resource> readResources() {
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

    ensure(behavior.isInstruction(), "Behavior is not a valid instruction behavior");

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
