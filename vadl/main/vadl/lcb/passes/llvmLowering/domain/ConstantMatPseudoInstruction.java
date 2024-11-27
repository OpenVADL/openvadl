package vadl.lcb.passes.llvmLowering.domain;

import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.viam.Assembly;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;

/**
 * A {@link ConstantMatPseudoInstruction} is like a {@link PseudoInstruction} except it is not
 * part of the assembler.
 */
public class ConstantMatPseudoInstruction extends PseudoInstruction {

  private final TableGenImmediateRecord immediateRecord;

  /**
   * Instantiates a PseudoInstruction object and verifies it.
   *
   * @param identifier     the identifier of the pseudo instruction
   * @param parameters     the list of parameters for the pseudo instruction
   * @param behavior       the behavior graph of the pseudo instruction
   */
  public ConstantMatPseudoInstruction(Identifier identifier,
                                      Parameter[] parameters,
                                      Graph behavior,
                                      Assembly assembly,
                                      TableGenImmediateRecord imm) {
    super(identifier, parameters, behavior, assembly);
    this.immediateRecord = imm;
  }

  public TableGenImmediateRecord immediateRecord() {
    return immediateRecord;
  }
}
