package vadl.iss.passes.decode;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.decode.dto.ArgumentSet;
import vadl.iss.passes.decode.dto.Field;
import vadl.iss.passes.decode.dto.FieldSlice;
import vadl.iss.passes.decode.dto.Pattern;
import vadl.iss.passes.decode.dto.QemuDecodeLoweringPassResult;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;

/**
 * Lowering pass that converts VADL form    s and instructions to QEMU decode definitions,
 * i.e. patterns, formats, argument sets and fields.
 */
public class QemuDecodeLoweringPass extends AbstractIssPass {

  /**
   * Constructor for the QEMU Decode Lowering Pass.
   *
   * @param configuration the configuration
   */
  public QemuDecodeLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  /**
   * Returns the name of the pass.
   *
   * @return the name of the pass
   */
  @Override
  public PassName getName() {
    return PassName.of("QEMU Decode Lowering");
  }

  /**
   * Executes the pass.
   *
   * @param passResults are the results from the different passes which have been executed so far.
   * @param viam        is latest VADL specification. Note that transformation passes are allowed
   *                    to mutate the object.
   * @return the result of the pass
   * @throws IOException if an IO error occurs during the execution of the pass
   */
  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      throw new ViamError("No ISA found in the specification");
    }

    final List<Format> formats = isa.ownFormats();
    final List<Instruction> instructions = isa.ownInstructions();

    if (formats.isEmpty() || instructions.isEmpty()) {
      // Nothing to do
      return null;
    }

    final Map<Identifier, ArgumentSet> qArgs = new HashMap<>();
    final Map<Pair<Identifier, BigInteger>, vadl.iss.passes.decode.dto.Format> qFormats =
        new HashMap<>();
    final List<Pattern> qPatterns = new ArrayList<>();

    for (Instruction i : instructions) {
      final var formatKey = Pair.of(i.format().identifier, getFixedBits(i));

      // The argument set only depends on the source VADL format
      final var argSet =
          qArgs.computeIfAbsent(i.format().identifier, k -> mapArgumentSet(i.format()));

      // The format definition depends on the fixed-bits mask as well
      final var format =
          qFormats.computeIfAbsent(formatKey, k -> mapFormat(i.format(), k.right(), argSet));

      qPatterns.add(new Pattern(i, format));
    }

    return new QemuDecodeLoweringPassResult(new ArrayList<>(qArgs.values()),
        new ArrayList<>(qFormats.values()), qPatterns);
  }

  private ArgumentSet mapArgumentSet(Format format) {
    final List<Field> fields =
        new ArrayList<>(Arrays.stream(format.fields()).map(this::mapField).toList());

    /* A field access function references a single encoded field in the format, and converts it
     * using some (for this sake) arbitrary logic.
     *
     * In QEMU a field access has to be its own field with a 'decodeFunction'
     *
     * If the field access is a trivial sign extension, we can encode this in the field directly,
     * however it would also be correct to use an extraction function for this. */
    for (Format.FieldAccess access : format.fieldAccesses()) {

      final Format.Field field = access.fieldRef();

      // Map the referenced field to obtain the slices
      final Field tmp = mapField(field);

      if (!isTrivialSignExtension(access)) {
        // Construct a pseudo field for the field access function. The name of the decode function
        // will be resolved in a follow-up pass
        final Field pseudoField = new Field(access, tmp.getSlices());
        fields.add(pseudoField);
        continue;
      }

      // We can encode the sign extension in the field itself, rather than using a function
      final List<FieldSlice> slices = tmp.getSlices();
      if (slices.isEmpty()) {
        throw new ViamError("Field has no slices");
      }
      final FieldSlice signedSlice = slices.get(0);

      // Replace the first slice with a signed one
      slices.remove(0);
      slices.add(0, new FieldSlice(signedSlice.start(), signedSlice.length(), true));
    }

    return new ArgumentSet(format, fields);
  }

  private Field mapField(Format.Field field) {
    final boolean signed = field.type().isSigned();

    // Prepare the slices
    final List<Constant.BitSlice.Part> parts = field.bitSlice().parts()
        .toList();

    // If the field is signed, the leftmost slice contains the sign bit
    final List<FieldSlice> slices = new ArrayList<>();
    for (int i = 0; i < parts.size(); i++) {
      var p = parts.get(i);
      slices.add(new FieldSlice(p.lsb(), p.size(), signed && i == 0));
    }

    return new Field(field, slices);
  }

  private vadl.iss.passes.decode.dto.Format mapFormat(Format format, BigInteger fixedBits,
                                                      ArgumentSet args) {

    // We may only extract fields which are not fixed in the corresponding patterns
    List<Field> fields = args.getFields().stream()
        .filter(f ->
            // The field is not 'fixed' if it does not overlap with the fixed bit mask
            f.getSlices().stream().anyMatch(s -> {
              BigInteger m = getBitMask(s.start(), s.start() + s.length() - 1);
              return m.and(fixedBits).equals(BigInteger.ZERO);
            }))
        .toList();
    return new vadl.iss.passes.decode.dto.Format(format, args, fields);
  }

  /**
   * Returns a bit mask where only the fixed bits in an instruction encoding are set.
   *
   * @param insn The instruction
   * @return The fixed bit mask
   */
  private BigInteger getFixedBits(Instruction insn) {
    BigInteger result = BigInteger.ZERO;
    for (Encoding.Field encField : insn.encoding().fieldEncodings()) {
      List<Constant.BitSlice.Part> parts = encField.formatField().bitSlice().parts().toList();
      for (Constant.BitSlice.Part p : parts) {
        for (int i = p.lsb(); i <= p.msb(); i++) {
          result = result.setBit(i);
        }
      }
    }
    return result;
  }

  /**
   * Returns a bit mask where all bits from the lsb to the msb are set
   *
   * @param lsb The least significant bit
   * @param msb The most significant bit
   * @return The bit mask
   */
  private BigInteger getBitMask(int lsb, int msb) {
    BigInteger result = BigInteger.ZERO;
    for (int i = lsb; i <= msb; i++) {
      result = result.setBit(i);
    }
    return result;
  }

  private boolean isTrivialSignExtension(Format.FieldAccess access) {
    final Type targetType = access.type();
    if (!targetType.isData() || !targetType.asDataType().isSigned()) {
      return false;
    }

    final DataType sourceType = access.fieldRef().type();
    if (sourceType.isSigned()) {
      // No sign extension needed
      return false;
    }

    final Graph behavior = access.accessFunction().behavior();

    if (!behavior.isPureFunction()) {
      // Contains control flow or side effects, cannot be a trivial sign extension
      return false;
    }

    // TODO: Check if the function is a trivial sign extension

    return false;
  }
}
