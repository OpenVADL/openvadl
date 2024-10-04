package vadl.lcb.passes.llvmLowering.domain;

import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;

/**
 * A {@link ConstantMatPseudoInstruction} is like a {@link PseudoInstruction} except it is not
 * part of the assembler.
 */
public class ConstantMatPseudoInstruction extends PseudoInstruction {

  private final Instruction instructionRef;
  private final TableGenImmediateRecord immediateRecord;

  /**
   * Instantiates a PseudoInstruction object and verifies it.
   *
   * @param identifier     the identifier of the pseudo instruction
   * @param parameters     the list of parameters for the pseudo instruction
   * @param behavior       the behavior graph of the pseudo instruction
   * @param instructionRef the reference to the machine instruction.
   * @param imm
   */
  public ConstantMatPseudoInstruction(Identifier identifier,
                                      Parameter[] parameters,
                                      Graph behavior,
                                      Instruction instructionRef,
                                      TableGenImmediateRecord imm) {
    super(identifier, parameters, behavior, instructionRef.assembly());
    this.instructionRef = instructionRef;
    this.immediateRecord = imm;
  }

  public Instruction instructionRef() {
    return instructionRef;
  }

  public TableGenImmediateRecord immediateRecord() {
    return immediateRecord;
  }
}
