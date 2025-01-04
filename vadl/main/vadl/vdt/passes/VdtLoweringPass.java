package vadl.vdt.passes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.theiling.TheilingDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * Lowering pass that creates the VDT (VADL Decode Tree) from the VIAM definition.
 */
public class VdtLoweringPass extends AbstractIssPass {

  /**
   * Constructor for the VDT Lowering Pass.
   *
   * @param configuration the configuration
   */
  public VdtLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Lowering");
  }

  @Override
  public @Nullable Node execute(PassResults passResults, Specification viam)
      throws IOException {

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      throw new ViamError("No ISA found in the specification");
    }

    var insns = isa.ownInstructions()
        .stream()
        .map(this::prepareInstruction)
        .toList();

    return new TheilingDecodeTreeGenerator().generate(insns);
  }

  /**
   * Prepares an instruction for the decode tree generation.
   *
   * @param insn The VIAM instruction
   * @return The prepared instruction
   */
  private Instruction prepareInstruction(vadl.viam.Instruction insn) {
    BitPattern pattern = getInsnPattern(insn);
    return new Instruction(insn, pattern.width(), pattern);
  }

  /**
   * Returns a bit pattern, where fixed bits in the instruction encoding are set to their respective
   * encoding value. All other bits are set to <i>don't care</i>.
   *
   * @param insn The instruction
   * @return The bit pattern
   */
  private BitPattern getInsnPattern(vadl.viam.Instruction insn) {

    final PBit[] bits = new PBit[insn.format().type().bitWidth()];

    // Initialize all bits to "don't care"
    for (int i = 0; i < bits.length; i++) {
      bits[i] = new PBit(PBit.Value.DONT_CARE);
    }

    // Set fixed bits to their respective encoding value
    for (Encoding.Field encField : insn.encoding().fieldEncodings()) {
      BigInteger fixedValue = encField.constant().integer();
      List<Constant.BitSlice.Part> parts = encField.formatField().bitSlice().parts().toList();
      for (Constant.BitSlice.Part p : parts) {
        for (int i = p.lsb(); i <= p.msb(); i++) {
          var val = fixedValue.testBit(i - p.lsb()) ? PBit.Value.ONE : PBit.Value.ZERO;
          bits[bits.length - (i + 1)] = new PBit(val);
        }
      }
    }

    return new BitPattern(bits);
  }
}
